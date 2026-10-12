(function () {
    'use strict';

    function setText(id, value) {
        document.getElementById(id).textContent = value;
    }

    async function loadStats() {
        try {
            const stats = await HMS.api('/api/dashboard/stats');
            setText('statPatients', stats.patients);
            setText('statDoctors', stats.doctors);
            setText('statToday', stats.todayAppointments);
            setText('statScheduled', stats.scheduledAppointments);
            if (stats.medicalRecords !== null && stats.medicalRecords !== undefined) {
                setText('statRecords', stats.medicalRecords);
                document.getElementById('recordsCard').classList.remove('hidden');
            }
        } catch (e) {
            HMS.toast(e.message, 'error');
            ['statPatients', 'statDoctors', 'statToday', 'statScheduled'].forEach(function (id) { setText(id, '?'); });
        }
    }

    async function loadToday() {
        const body = document.getElementById('todayBody');
        try {
            const result = await HMS.api('/api/appointments?date=' + HMS.todayIso() + '&size=20');
            if (result.content.length === 0) {
                body.innerHTML = '<tr><td colspan="5" class="empty">No appointments today.</td></tr>';
                return;
            }
            body.innerHTML = result.content.map(function (a) {
                return '<tr>'
                    + '<td>' + HMS.escape(HMS.time(a.startTime)) + '</td>'
                    + '<td>' + HMS.escape(a.patientName) + '</td>'
                    + '<td>' + HMS.escape(a.doctorName) + '</td>'
                    + '<td class="wrap">' + HMS.escape(a.reason) + '</td>'
                    + '<td><span class="badge ' + HMS.escape(a.status.toLowerCase()) + '">' + HMS.escape(HMS.label(a.status)) + '</span></td>'
                    + '</tr>';
            }).join('');
        } catch (e) {
            body.innerHTML = '<tr><td colspan="5" class="empty">Could not load appointments.</td></tr>';
        }
    }

    (async function start() {
        const me = await HMS.init('dashboard');
        if (!me) return;
        document.getElementById('welcome').textContent = 'Welcome, ' + me.fullName + '.';
        loadStats();
        loadToday();
    })();
})();
