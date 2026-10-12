(function () {
    'use strict';

    const API = '/api/medical-records';
    const form = document.getElementById('recordForm');
    const idInput = document.getElementById('recordId');
    const body = document.getElementById('recordBody');
    const patientError = form.querySelector('[data-error-for="patientId"]');
    const state = { page: 0, size: 10, search: '' };
    let rows = {};
    let me = null;
    let picker = null;
    let isDoctor = false;
    let isAdmin = false;

    async function load() {
        try {
            const result = await HMS.api(API + '?search=' + encodeURIComponent(state.search)
                + '&page=' + state.page + '&size=' + state.size);
            rows = {};
            result.content.forEach(function (r) { rows[r.id] = r; });
            render(result.content);
            HMS.renderPager(document.getElementById('pager'), result, function (p) { state.page = p; load(); });
        } catch (e) {
            body.innerHTML = '<tr><td colspan="7" class="empty">Could not load records.</td></tr>';
            HMS.toast(e.message, 'error');
        }
    }

    function actionButtons(r) {
        let html = '';
        // a doctor may only edit records they wrote; the server enforces this too
        if (isDoctor && me.doctorId === r.doctorId) {
            html += '<button class="btn small" data-action="edit" data-id="' + r.id + '">Edit</button>';
        }
        if (isAdmin) {
            html += '<button class="btn small danger" data-action="delete" data-id="' + r.id + '">Delete</button>';
        }
        return html || '-';
    }

    function render(list) {
        if (list.length === 0) {
            body.innerHTML = '<tr><td colspan="7" class="empty">No medical records found.</td></tr>';
            return;
        }
        body.innerHTML = list.map(function (r) {
            return '<tr>'
                + '<td>' + HMS.escape(r.recordDate) + '</td>'
                + '<td>' + HMS.escape(r.patientName) + '</td>'
                + '<td>' + HMS.escape(r.doctorName) + '</td>'
                + '<td class="wrap">' + HMS.escape(r.diagnosis) + '</td>'
                + '<td class="wrap">' + HMS.escape(r.treatment) + '</td>'
                + '<td class="wrap">' + HMS.escape(r.prescription || '-') + '</td>'
                + '<td class="actions">' + actionButtons(r) + '</td>'
                + '</tr>';
        }).join('');
    }

    function resetForm() {
        form.reset();
        idInput.value = '';
        picker.clear();
        HMS.clearErrors(form);
        patientError.textContent = '';
        document.getElementById('formTitle').textContent = 'Add medical record';
        document.getElementById('recordDate').value = HMS.todayIso();
    }

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        HMS.clearErrors(form);
        patientError.textContent = '';

        const patientId = picker.getId();
        if (!patientId) {
            patientError.textContent = 'Pick a patient from the suggestions.';
            return;
        }
        const data = HMS.formToObject(form);
        data.patientId = patientId;

        const id = idInput.value;
        const saveBtn = document.getElementById('saveBtn');
        saveBtn.disabled = true;
        try {
            await HMS.api(id ? API + '/' + id : API, { method: id ? 'PUT' : 'POST', body: data });
            HMS.toast(id ? 'Record updated.' : 'Record added.', 'success');
            resetForm();
            load();
        } catch (e) {
            HMS.showErrors(form, e.fieldErrors);
            if (e.fieldErrors && e.fieldErrors.patientId) patientError.textContent = e.fieldErrors.patientId;
            HMS.toast(e.message, 'error');
        } finally {
            saveBtn.disabled = false;
        }
    });

    document.getElementById('cancelBtn').addEventListener('click', resetForm);

    document.getElementById('searchBox').addEventListener('input', HMS.debounce(function (event) {
        state.search = event.target.value.trim();
        state.page = 0;
        load();
    }, 300));

    body.addEventListener('click', async function (event) {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const id = button.dataset.id;
        if (button.dataset.action === 'edit') {
            const r = rows[id];
            HMS.fillForm(form, r);
            picker.setPatient(r.patientId, r.patientName);
            idInput.value = id;
            document.getElementById('formTitle').textContent = 'Edit medical record';
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } else if (button.dataset.action === 'delete') {
            if (!window.confirm('Delete this medical record? It will be hidden but kept in the database.')) return;
            try {
                await HMS.api(API + '/' + id, { method: 'DELETE' });
                HMS.toast('Record deleted.', 'success');
                load();
            } catch (e) {
                HMS.toast(e.message, 'error');
            }
        }
    });

    (async function start() {
        me = await HMS.init('medical-records');
        if (!me) return;
        isDoctor = me.role === 'DOCTOR';
        isAdmin = me.role === 'ADMIN';
        picker = HMS.patientPicker(document.getElementById('patientLookup'), document.getElementById('patientOptions'));

        if (isDoctor) {
            document.getElementById('formCard').classList.remove('hidden');
            HMS.prepareForm(form);
            document.getElementById('recordDate').value = HMS.todayIso();
            document.getElementById('recordDate').max = HMS.todayIso();
        } else {
            document.getElementById('subtitle').textContent =
                'Read-only for administrators. Only doctors can write records.';
        }
        load();
    })();
})();
