(function () {
    'use strict';

    const API = '/api/users';
    const form = document.getElementById('userForm');
    const body = document.getElementById('userBody');
    const roleSelect = document.getElementById('role');
    const doctorWrap = document.getElementById('doctorWrap');
    let rows = {};
    let me = null;

    async function loadDoctors() {
        try {
            const doctors = await HMS.api('/api/doctors/options');
            const select = document.getElementById('doctorId');
            doctors.forEach(function (d) {
                const option = document.createElement('option');
                option.value = d.id;
                option.textContent = d.label;
                select.appendChild(option);
            });
        } catch (e) {
            HMS.toast(e.message, 'error');
        }
    }

    async function load() {
        try {
            const users = await HMS.api(API);
            rows = {};
            users.forEach(function (u) { rows[u.id] = u; });
            render(users);
        } catch (e) {
            body.innerHTML = '<tr><td colspan="6" class="empty">Could not load users.</td></tr>';
            HMS.toast(e.message, 'error');
        }
    }

    function render(users) {
        if (users.length === 0) {
            body.innerHTML = '<tr><td colspan="6" class="empty">No users.</td></tr>';
            return;
        }
        body.innerHTML = users.map(function (u) {
            const isSelf = u.username === me.username;
            const toggle = isSelf ? '' :
                '<button class="btn small ' + (u.enabled ? 'danger' : 'success') + '" data-action="toggle" data-id="' + u.id + '">'
                + (u.enabled ? 'Disable' : 'Enable') + '</button>';
            return '<tr>'
                + '<td>' + HMS.escape(u.username) + '</td>'
                + '<td>' + HMS.escape(u.fullName) + '</td>'
                + '<td>' + HMS.escape(HMS.label(u.role)) + '</td>'
                + '<td>' + HMS.escape(u.doctorName || '-') + '</td>'
                + '<td><span class="badge ' + (u.enabled ? 'on' : 'off') + '">' + (u.enabled ? 'Active' : 'Disabled') + '</span></td>'
                + '<td class="actions">' + toggle
                + '<button class="btn small secondary" data-action="password" data-id="' + u.id + '">Reset password</button></td>'
                + '</tr>';
        }).join('');
    }

    roleSelect.addEventListener('change', function () {
        doctorWrap.classList.toggle('hidden', roleSelect.value !== 'DOCTOR');
        if (roleSelect.value !== 'DOCTOR') document.getElementById('doctorId').value = '';
    });

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        HMS.clearErrors(form);
        const data = HMS.formToObject(form);
        data.doctorId = data.doctorId ? Number(data.doctorId) : null;
        const saveBtn = document.getElementById('saveBtn');
        saveBtn.disabled = true;
        try {
            await HMS.api(API, { method: 'POST', body: data });
            HMS.toast('User created.', 'success');
            form.reset();
            doctorWrap.classList.add('hidden');
            load();
        } catch (e) {
            HMS.showErrors(form, e.fieldErrors);
            HMS.toast(e.message, 'error');
        } finally {
            saveBtn.disabled = false;
        }
    });

    body.addEventListener('click', async function (event) {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const user = rows[button.dataset.id];
        if (button.dataset.action === 'toggle') {
            const enable = !user.enabled;
            if (!window.confirm((enable ? 'Enable' : 'Disable') + ' the account "' + user.username + '"?')) return;
            try {
                await HMS.api(API + '/' + user.id + '/enabled', { method: 'PATCH', body: { enabled: enable } });
                HMS.toast('Account ' + (enable ? 'enabled' : 'disabled') + '.', 'success');
                load();
            } catch (e) {
                HMS.toast(e.message, 'error');
            }
        } else if (button.dataset.action === 'password') {
            const password = window.prompt('New password for "' + user.username + '" (8+ characters, with a letter and a digit):');
            if (!password) return;
            try {
                await HMS.api(API + '/' + user.id + '/password', { method: 'PUT', body: { password: password } });
                HMS.toast('Password changed.', 'success');
            } catch (e) {
                HMS.toast(e.fieldErrors && e.fieldErrors.password ? e.fieldErrors.password : e.message, 'error');
            }
        }
    });

    (async function start() {
        me = await HMS.init('users');
        if (!me) return;
        HMS.prepareForm(form);
        await loadDoctors();
        load();
    })();
})();
