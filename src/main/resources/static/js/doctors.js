(function () {
    'use strict';

    const API = '/api/doctors';
    const form = document.getElementById('doctorForm');
    const idInput = document.getElementById('doctorId');
    const body = document.getElementById('doctorBody');
    const state = { page: 0, size: 10, search: '' };
    let rows = {};
    let isAdmin = false;

    async function load() {
        try {
            const result = await HMS.api(API + '?search=' + encodeURIComponent(state.search)
                + '&page=' + state.page + '&size=' + state.size);
            rows = {};
            result.content.forEach(function (d) { rows[d.id] = d; });
            render(result.content);
            HMS.renderPager(document.getElementById('pager'), result, function (p) { state.page = p; load(); });
        } catch (e) {
            body.innerHTML = '<tr><td colspan="8" class="empty">Could not load doctors.</td></tr>';
            HMS.toast(e.message, 'error');
        }
    }

    function render(list) {
        if (list.length === 0) {
            body.innerHTML = '<tr><td colspan="8" class="empty">No doctors found.</td></tr>';
            return;
        }
        body.innerHTML = list.map(function (d) {
            return '<tr>'
                + '<td>' + HMS.escape(d.fullName) + '</td>'
                + '<td>' + HMS.escape(d.specialization) + '</td>'
                + '<td class="wrap">' + HMS.escape(d.qualification || '-') + '</td>'
                + '<td>' + HMS.escape(d.experienceYears) + ' yrs</td>'
                + '<td>' + HMS.escape(HMS.time(d.availableFrom)) + ' - ' + HMS.escape(HMS.time(d.availableTo)) + '</td>'
                + '<td>' + HMS.escape(d.phone) + '</td>'
                + '<td>' + HMS.escape(d.email) + '</td>'
                + (isAdmin
                    ? '<td class="actions">'
                        + '<button class="btn small" data-action="edit" data-id="' + d.id + '">Edit</button>'
                        + '<button class="btn small danger" data-action="delete" data-id="' + d.id + '">Delete</button>'
                        + '</td>'
                    : '')
                + '</tr>';
        }).join('');
    }

    function resetForm() {
        form.reset();
        idInput.value = '';
        HMS.clearErrors(form);
        document.getElementById('formTitle').textContent = 'Add doctor';
    }

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        HMS.clearErrors(form);
        const id = idInput.value;
        const saveBtn = document.getElementById('saveBtn');
        saveBtn.disabled = true;
        try {
            await HMS.api(id ? API + '/' + id : API, { method: id ? 'PUT' : 'POST', body: HMS.formToObject(form) });
            HMS.toast(id ? 'Doctor updated.' : 'Doctor added.', 'success');
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
            const d = Object.assign({}, rows[id]);
            d.availableFrom = HMS.time(d.availableFrom);
            d.availableTo = HMS.time(d.availableTo);
            HMS.fillForm(form, d);
            idInput.value = id;
            document.getElementById('formTitle').textContent = 'Edit doctor';
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } else if (button.dataset.action === 'delete') {
            if (!window.confirm('Delete this doctor? This is blocked while they have scheduled appointments.')) return;
            try {
                await HMS.api(API + '/' + id, { method: 'DELETE' });
                HMS.toast('Doctor deleted.', 'success');
                load();
            } catch (e) {
                HMS.toast(e.message, 'error');
            }
        }
    });

    (async function start() {
        const me = await HMS.init('doctors');
        if (!me) return;
        isAdmin = HMS.can('ADMIN');
        if (isAdmin) {
            document.getElementById('formCard').classList.remove('hidden');
            HMS.prepareForm(form);
        } else {
            document.getElementById('actionsHead').classList.add('hidden');
        }
        load();
    })();
})();
