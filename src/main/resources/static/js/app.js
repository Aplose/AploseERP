/* AploseERP - Application JavaScript */

// CSRF token for AJAX requests
const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

// Auto-dismiss alerts after 5 seconds
document.querySelectorAll('.alert-dismissible').forEach(alert => {
    setTimeout(() => {
        const bsAlert = new bootstrap.Alert(alert);
        if (bsAlert) bsAlert.close();
    }, 5000);
});

// Confirm delete dialogs
document.querySelectorAll('[data-confirm]').forEach(el => {
    el.addEventListener('click', function(e) {
        if (!confirm(this.dataset.confirm || 'Are you sure?')) {
            e.preventDefault();
        }
    });
});
