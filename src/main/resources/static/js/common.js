/*
 * Shared helpers used by every page.
 *  - HMS.api():   fetch wrapper that sends the CSRF header, parses JSON and throws readable errors
 *  - HMS.escape(): MUST be used for every value put into innerHTML (prevents stored XSS)
 *  - HMS.init():  loads the logged-in user and builds the role-aware navigation
 */
(function () {
    'use strict';

    class ApiError extends Error {
        constructor(status, message, fieldErrors) {
            super(message);
            this.status = status;
            this.fieldErrors = fieldErrors || null;
        }
    }

    const NAV_LINKS = [
        { href: 'dashboard.html', key: 'dashboard', label: 'Dashboard', roles: null },
        { href: 'patients.html', key: 'patients', label: 'Patients', roles: null },
        { href: 'doctors.html', key: 'doctors', label: 'Doctors', roles: null },
        { href: 'appointments.html', key: 'appointments', label: 'Appointments', roles: null },
        { href: 'patient-profile.html', key: 'patient-profile', label: 'Patient profile', roles: ['ADMIN', 'RECEPTIONIST', 'DOCTOR'] },
        { href: 'queue.html', key: 'queue', label: 'Visit queue', roles: ['ADMIN', 'RECEPTIONIST', 'DOCTOR'] },
        { href: 'consultation.html', key: 'consultation', label: 'Consultation', roles: ['ADMIN', 'DOCTOR'] },
        { href: 'prescriptions.html', key: 'prescriptions', label: 'Prescriptions', roles: ['ADMIN', 'DOCTOR'] },
        { href: 'billing.html', key: 'billing', label: 'Billing', roles: ['ADMIN', 'RECEPTIONIST'] },
        { href: 'medical-records.html', key: 'medical-records', label: 'Medical Records', roles: ['ADMIN', 'DOCTOR'] },
        { href: 'users.html', key: 'users', label: 'Users', roles: ['ADMIN'] }
    ];

    const ROLE_LABELS = { ADMIN: 'Administrator', DOCTOR: 'Doctor', RECEPTIONIST: 'Receptionist' };

    let currentUser = null;
    let toastTimer = null;

    function getCookie(name) {
        const pair = document.cookie.split('; ').find(function (row) { return row.startsWith(name + '='); });
        return pair ? decodeURIComponent(pair.substring(name.length + 1)) : null;
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) return '';
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    async function api(path, options) {
        const opts = options || {};
        const method = opts.method || 'GET';
        const headers = { 'Accept': 'application/json' };
        if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
        if (method !== 'GET') {
            const token = getCookie('XSRF-TOKEN');
            if (token) headers['X-XSRF-TOKEN'] = token;
        }

        let response;
        try {
            response = await fetch(path, {
                method: method,
                headers: headers,
                body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
                credentials: 'same-origin'
            });
        } catch (networkError) {
            throw new ApiError(0, 'Cannot reach the server. Check your connection.');
        }

        if (response.status === 401) {
            window.location.href = '/login.html';
            throw new ApiError(401, 'Please log in.');
        }

        const text = await response.text();
        let data = null;
        if (text) {
            try { data = JSON.parse(text); } catch (e) { data = null; }
        }
        if (!response.ok) {
            const message = (data && data.message) || 'Request failed (' + response.status + ').';
            throw new ApiError(response.status, message, data && data.fieldErrors);
        }
        return data;
    }

    function toast(message, type) {
        const el = document.getElementById('toast');
        if (!el) return;
        el.textContent = message;
        el.className = 'toast ' + (type || '');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () { el.classList.add('hidden'); }, 4500);
    }

    function can() {
        if (!currentUser) return false;
        return Array.prototype.slice.call(arguments).indexOf(currentUser.role) !== -1;
    }

    function renderNav(user, activeKey) {
        const nav = document.getElementById('mainNav');
        if (nav) {
            nav.textContent = '';
            NAV_LINKS.forEach(function (link) {
                if (link.roles && link.roles.indexOf(user.role) === -1) return;
                const a = document.createElement('a');
                a.href = link.href;
                a.textContent = link.label;
                if (link.key === activeKey) a.className = 'active';
                nav.appendChild(a);
            });
        }
        const box = document.getElementById('userBox');
        if (box) {
            box.textContent = '';
            const name = document.createElement('span');
            name.textContent = user.fullName;
            const tag = document.createElement('span');
            tag.className = 'role-tag';
            tag.textContent = ROLE_LABELS[user.role] || user.role;
            const logout = document.createElement('button');
            logout.type = 'button';
            logout.className = 'btn light small';
            logout.textContent = 'Logout';
            logout.addEventListener('click', doLogout);
            box.appendChild(name);
            box.appendChild(tag);
            box.appendChild(logout);
        }
    }

    async function doLogout() {
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '' },
                credentials: 'same-origin'
            });
        } finally {
            window.location.href = '/login.html';
        }
    }

    async function init(activeKey) {
        let user;
        try {
            user = await api('/api/auth/me');
        } catch (e) {
            return null;
        }
        currentUser = user;
        renderNav(user, activeKey);
        if (new URLSearchParams(window.location.search).get('denied')) {
            toast('Your role does not have access to that page.', 'error');
        }
        return user;
    }

    /* ---------- forms ---------- */

    /** Adds an empty <span class="field-error"> after every named input so server messages have a home. */
    function prepareForm(form) {
        Array.prototype.forEach.call(form.elements, function (el) {
            if (!el.name || el.dataset.errorSlot) return;
            const slot = document.createElement('span');
            slot.className = 'field-error';
            slot.dataset.errorFor = el.name;
            el.insertAdjacentElement('afterend', slot);
            el.dataset.errorSlot = '1';
        });
    }

    function clearErrors(form) {
        form.querySelectorAll('.field-error').forEach(function (s) { s.textContent = ''; });
    }

    function showErrors(form, fieldErrors) {
        if (!fieldErrors) return;
        Object.keys(fieldErrors).forEach(function (field) {
            const slot = form.querySelector('.field-error[data-error-for="' + field + '"]');
            if (slot) slot.textContent = fieldErrors[field];
        });
    }

    /** Reads named inputs into a plain object. Empty strings become null; number inputs become numbers. */
    function formToObject(form) {
        const data = {};
        Array.prototype.forEach.call(form.elements, function (el) {
            if (!el.name) return;
            let value = typeof el.value === 'string' ? el.value.trim() : el.value;
            if (value === '') value = null;
            else if (el.type === 'number') value = Number(value);
            data[el.name] = value;
        });
        return data;
    }

    function fillForm(form, values) {
        Array.prototype.forEach.call(form.elements, function (el) {
            if (!el.name) return;
            const v = values[el.name];
            el.value = (v === null || v === undefined) ? '' : String(v);
        });
    }

    /* ---------- tables ---------- */

    function renderPager(container, pageData, onChange) {
        container.textContent = '';
        if (!pageData || pageData.totalElements === 0) return;
        const from = pageData.page * pageData.size + 1;
        const to = Math.min((pageData.page + 1) * pageData.size, pageData.totalElements);

        const info = document.createElement('div');
        info.className = 'info';
        info.textContent = 'Showing ' + from + '-' + to + ' of ' + pageData.totalElements;

        const buttons = document.createElement('div');
        buttons.className = 'buttons';
        const prev = document.createElement('button');
        prev.type = 'button';
        prev.className = 'btn secondary small';
        prev.textContent = 'Previous';
        prev.disabled = pageData.page <= 0;
        prev.addEventListener('click', function () { onChange(pageData.page - 1); });
        const next = document.createElement('button');
        next.type = 'button';
        next.className = 'btn secondary small';
        next.textContent = 'Next';
        next.disabled = pageData.page + 1 >= pageData.totalPages;
        next.addEventListener('click', function () { onChange(pageData.page + 1); });
        buttons.appendChild(prev);
        buttons.appendChild(next);

        container.appendChild(info);
        container.appendChild(buttons);
    }

    function debounce(fn, delay) {
        let timer;
        return function () {
            const args = arguments;
            clearTimeout(timer);
            timer = setTimeout(function () { fn.apply(null, args); }, delay);
        };
    }

    /* ---------- small formatters ---------- */

    function time(t) { return t ? String(t).substring(0, 5) : ''; }

    function label(enumValue) {
        if (!enumValue) return '';
        const s = String(enumValue).toLowerCase();
        return s.charAt(0).toUpperCase() + s.slice(1);
    }

    function todayIso() {
        const d = new Date();
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
    }

    /**
     * Search-as-you-type patient picker built on <input list="..."> + <datalist>.
     * Returns { getId(), setPatient(id, name), clear() }.
     */
    function patientPicker(input, datalist) {
        const pattern = /\(#(\d+)\)\s*$/;
        const load = debounce(async function () {
            try {
                const options = await api('/api/patients/options?search=' + encodeURIComponent(input.value.replace(pattern, '').trim()));
                datalist.textContent = '';
                options.forEach(function (o) {
                    const opt = document.createElement('option');
                    opt.value = o.label + ' (#' + o.id + ')';
                    datalist.appendChild(opt);
                });
            } catch (e) { /* suggestions are optional */ }
        }, 250);
        input.addEventListener('input', load);
        input.addEventListener('focus', load);
        return {
            getId: function () {
                const m = input.value.match(pattern);
                return m ? Number(m[1]) : null;
            },
            setPatient: function (id, name) { input.value = name + ' (#' + id + ')'; },
            clear: function () { input.value = ''; }
        };
    }

    window.HMS = {
        ApiError: ApiError,
        api: api,
        escape: escapeHtml,
        toast: toast,
        init: init,
        can: can,
        prepareForm: prepareForm,
        clearErrors: clearErrors,
        showErrors: showErrors,
        formToObject: formToObject,
        fillForm: fillForm,
        renderPager: renderPager,
        debounce: debounce,
        time: time,
        label: label,
        todayIso: todayIso,
        patientPicker: patientPicker,
        getCookie: getCookie
    };
})();
