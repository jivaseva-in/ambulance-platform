/**
 * UI helpers: Alerts, notification toasts, and badge styling
 */
const alertBox = document.getElementById('alertBox');

export function showAlert(message, type = 'info') {
    if (!alertBox) return;

    let bg = 'bg-blue-100 text-blue-800 border border-blue-300';
    if (type === 'error') {
        bg = 'bg-red-100 text-red-800 border border-red-300';
    } else if (type === 'success') {
        bg = 'bg-green-100 text-green-800 border border-green-300';
    }

    alertBox.className = `p-4 rounded-md text-sm font-medium transition-all ${bg}`;
    alertBox.textContent = message;
    alertBox.classList.remove('hidden');

    setTimeout(() => {
        alertBox.classList.add('hidden');
    }, 5000);
}
