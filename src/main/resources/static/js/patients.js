(function () {
    'use strict';

    const API = '/api/patients';
    const form = document.getElementById('patientForm');
    const idInput = document.getElementById('patientId');
    const body = document.getElementById('patientBody');
    const state = { page: 0, size: 10, search: '' };
    let rows = {};
    let canWrite = false;
    let canDelete = false;

    async function load() {
        try {
            const result = await HMS.api(API + '?search=' + encodeURIComponent(state.search)
                + '&page=' + state.page + '&size=' + state.size);
            rows = {};
            result.content.forEach(function (p) { rows[p.id] = p; });
            render(result.content);
            HMS.renderPager(document.getElementById('pager'), result, function (p) { state.page = p; load(); });
        } catch (e) {
            body.innerHTML = '<tr><td colspan="8" class="empty">Could not load patients.</td></tr>';
            HMS.toast(e.message, 'error');
        }
    }

    function render(list) {
        if (list.length === 0) {
            body.innerHTML = '<tr><td colspan="8" class="empty">No patients found.</td></tr>';
            return;
        }
        body.innerHTML = list.map(function (p) {
            const emergency = p.emergencyContactName
                ? HMS.escape(p.emergencyContactName) + (p.emergencyContactPhone ? ' (' + HMS.escape(p.emergencyContactPhone) + ')' : '')
                : '-';
            let actions = '';
            if (canWrite) actions += '<button class="btn small" data-action="edit" data-id="' + p.id + '">Edit</button>';
            if (canDelete) actions += '<button class="btn small danger" data-action="delete" data-id="' + p.id + '">Delete</button>';
            return '<tr>'
                + '<td>' + HMS.escape(p.fullName) + '</td>'
                + '<td>' + HMS.escape(p.age) + '</td>'
                + '<td>' + HMS.escape(HMS.label(p.gender)) + '</td>'
                + '<td>' + HMS.escape(p.phone) + '</td>'
                + '<td>' + HMS.escape(p.email) + '</td>'
                + '<td>' + HMS.escape(p.bloodGroup || '-') + '</td>'
                + '<td class="wrap">' + emergency + '</td>'
                + (canWrite || canDelete ? '<td class="actions">' + actions + '</td>' : '')
                + '</tr>';
        }).join('');
    }

    function resetForm() {
        form.reset();
        idInput.value = '';
        HMS.clearErrors(form);
        document.getElementById('formTitle').textContent = 'Add patient';
    }

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        HMS.clearErrors(form);
        const id = idInput.value;
        const saveBtn = document.getElementById('saveBtn');
        saveBtn.disabled = true;
        try {
            await HMS.api(id ? API + '/' + id : API, { method: id ? 'PUT' : 'POST', body: HMS.formToObject(form) });
            HMS.toast(id ? 'Patient updated.' : 'Patient added.', 'success');
            resetForm();
            load();
        } catch (e) {
            HMS.showErrors(form, e.fieldErrors);
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
            HMS.fillForm(form, rows[id]);
            idInput.value = id;
            document.getElementById('formTitle').textContent = 'Edit patient';
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } else if (button.dataset.action === 'delete') {
            if (!window.confirm('Delete this patient? Their history is kept, but they will no longer appear in lists.')) return;
            try {
                await HMS.api(API + '/' + id, { method: 'DELETE' });
                HMS.toast('Patient deleted.', 'success');
                load();
            } catch (e) {
                HMS.toast(e.message, 'error');
            }
        }
    });

    (async function start() {
        const me = await HMS.init('patients');
        if (!me) return;
        canWrite = HMS.can('ADMIN', 'RECEPTIONIST');
        canDelete = HMS.can('ADMIN');
        if (canWrite) {
            document.getElementById('formCard').classList.remove('hidden');
            HMS.prepareForm(form);
        }
        if (!canWrite && !canDelete) document.getElementById('actionsHead').classList.add('hidden');
        load();
    })();
})();
