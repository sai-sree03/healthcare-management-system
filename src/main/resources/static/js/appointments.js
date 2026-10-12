(function () {
    'use strict';

    const API = '/api/appointments';
    const form = document.getElementById('appointmentForm');
    const idInput = document.getElementById('appointmentId');
    const body = document.getElementById('appointmentBody');
    const patientError = form.querySelector('[data-error-for="patientId"]');
    const state = { page: 0, size: 10, doctorId: '', date: '', status: '' };
    let rows = {};
    let me = null;
    let picker = null;

    async function loadDoctors() {
        try {
            const doctors = await HMS.api('/api/doctors/options');
            ['doctorId', 'filterDoctor'].forEach(function (selectId) {
                const select = document.getElementById(selectId);
                doctors.forEach(function (d) {
                    const option = document.createElement('option');
                    option.value = d.id;
                    option.textContent = d.label;
                    select.appendChild(option);
                });
            });
        } catch (e) {
            HMS.toast(e.message, 'error');
        }
    }

    async function load() {
        const params = new URLSearchParams({ page: state.page, size: state.size });
        if (state.doctorId) params.set('doctorId', state.doctorId);
        if (state.date) params.set('date', state.date);
        if (state.status) params.set('status', state.status);
        try {
            const result = await HMS.api(API + '?' + params.toString());
            rows = {};
            result.content.forEach(function (a) { rows[a.id] = a; });
            render(result.content);
            HMS.renderPager(document.getElementById('pager'), result, function (p) { state.page = p; load(); });
        } catch (e) {
            body.innerHTML = '<tr><td colspan="7" class="empty">Could not load appointments.</td></tr>';
            HMS.toast(e.message, 'error');
        }
    }

    function actionButtons(a) {
        if (a.status !== 'SCHEDULED') return '-';
        let html = '';
        if (HMS.can('ADMIN', 'RECEPTIONIST')) {
            html += '<button class="btn small" data-action="edit" data-id="' + a.id + '">Edit</button>';
        }
        if (HMS.can('ADMIN', 'DOCTOR')) {
            html += '<button class="btn small success" data-action="complete" data-id="' + a.id + '">Complete</button>';
        }
        html += '<button class="btn small danger" data-action="cancel" data-id="' + a.id + '">Cancel</button>';
        return html;
    }

    function render(list) {
        if (list.length === 0) {
            body.innerHTML = '<tr><td colspan="7" class="empty">No appointments found.</td></tr>';
            return;
        }
        body.innerHTML = list.map(function (a) {
            return '<tr>'
                + '<td>' + HMS.escape(a.appointmentDate) + '</td>'
                + '<td>' + HMS.escape(HMS.time(a.startTime)) + ' - ' + HMS.escape(HMS.time(a.endTime)) + '</td>'
                + '<td>' + HMS.escape(a.patientName) + '</td>'
                + '<td>' + HMS.escape(a.doctorName) + '</td>'
                + '<td class="wrap">' + HMS.escape(a.reason) + '</td>'
                + '<td><span class="badge ' + HMS.escape(a.status.toLowerCase()) + '">' + HMS.escape(HMS.label(a.status)) + '</span></td>'
                + '<td class="actions">' + actionButtons(a) + '</td>'
                + '</tr>';
        }).join('');
    }

    function resetForm() {
        form.reset();
        idInput.value = '';
        picker.clear();
        HMS.clearErrors(form);
        patientError.textContent = '';
        document.getElementById('formTitle').textContent = 'Book appointment';
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
        data.doctorId = data.doctorId ? Number(data.doctorId) : null;

        const id = idInput.value;
        const saveBtn = document.getElementById('saveBtn');
        saveBtn.disabled = true;
        try {
            await HMS.api(id ? API + '/' + id : API, { method: id ? 'PUT' : 'POST', body: data });
            HMS.toast(id ? 'Appointment updated.' : 'Appointment booked.', 'success');
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

    function bindFilters() {
        document.getElementById('filterDoctor').addEventListener('change', function (e) { state.doctorId = e.target.value; state.page = 0; load(); });
        document.getElementById('filterDate').addEventListener('change', function (e) { state.date = e.target.value; state.page = 0; load(); });
        document.getElementById('filterStatus').addEventListener('change', function (e) { state.status = e.target.value; state.page = 0; load(); });
        document.getElementById('clearFilters').addEventListener('click', function () {
            document.getElementById('filterDoctor').value = '';
            document.getElementById('filterDate').value = '';
            document.getElementById('filterStatus').value = '';
            state.doctorId = ''; state.date = ''; state.status = ''; state.page = 0;
            load();
        });
    }

    async function setStatus(id, status, question) {
        if (!window.confirm(question)) return;
        try {
            await HMS.api(API + '/' + id + '/status', { method: 'PATCH', body: { status: status } });
            HMS.toast('Appointment ' + status.toLowerCase() + '.', 'success');
            load();
        } catch (e) {
            HMS.toast(e.message, 'error');
        }
    }

    body.addEventListener('click', function (event) {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const id = button.dataset.id;
        const action = button.dataset.action;
        if (action === 'edit') {
            const a = rows[id];
            HMS.fillForm(form, { doctorId: a.doctorId, appointmentDate: a.appointmentDate, startTime: HMS.time(a.startTime), reason: a.reason });
            picker.setPatient(a.patientId, a.patientName);
            idInput.value = id;
            document.getElementById('formTitle').textContent = 'Edit appointment';
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } else if (action === 'complete') {
            setStatus(id, 'COMPLETED', 'Mark this appointment as completed?');
        } else if (action === 'cancel') {
            setStatus(id, 'CANCELLED', 'Cancel this appointment? This cannot be undone.');
        }
    });

    (async function start() {
        me = await HMS.init('appointments');
        if (!me) return;
        picker = HMS.patientPicker(document.getElementById('patientLookup'), document.getElementById('patientOptions'));

        if (HMS.can('ADMIN', 'RECEPTIONIST')) {
            document.getElementById('formCard').classList.remove('hidden');
            HMS.prepareForm(form);
            document.getElementById('appointmentDate').min = HMS.todayIso();
        }
        if (me.role === 'DOCTOR') {
            // the server only returns this doctor's own appointments, so the doctor filter is pointless
            document.getElementById('doctorFilterWrap').classList.add('hidden');
            document.getElementById('subtitle').textContent = 'Your appointments. You can complete or cancel them.';
        }
        bindFilters();
        await loadDoctors();
        load();
    })();
})();
