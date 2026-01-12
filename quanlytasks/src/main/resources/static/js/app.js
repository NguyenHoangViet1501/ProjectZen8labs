// ========================================
// Task Management System - JavaScript
// ========================================

document.addEventListener('DOMContentLoaded', function() {
    // Initialize tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Auto-hide alerts after 5 seconds
    setTimeout(function() {
        var alerts = document.querySelectorAll('.alert-dismissible');
        alerts.forEach(function(alert) {
            var bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        });
    }, 5000);
});

// Delete confirmation
function confirmDelete(message) {
    return confirm(message || 'Bạn có chắc chắn muốn xóa?');
}

// Format date to Vietnamese
function formatDate(dateString) {
    if (!dateString) return 'Không có';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Mark notification as read
async function markNotificationRead(notificationId, element) {
    try {
        const response = await fetch(`/api/notifications/${notificationId}/read`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (response.ok) {
            element.classList.remove('unread');
            updateNotificationBadge();
        }
    } catch (error) {
        console.error('Error marking notification as read:', error);
    }
}

// Update notification badge count
function updateNotificationBadge() {
    const badge = document.querySelector('.notification-badge .badge');
    if (badge) {
        let count = parseInt(badge.textContent) - 1;
        if (count <= 0) {
            badge.style.display = 'none';
        } else {
            badge.textContent = count;
        }
    }
}

// Toggle subtask completion
async function toggleSubtask(subtaskId, checkbox) {
    const status = checkbox.checked ? 'DONE' : 'TODO';
    try {
        const response = await fetch(`/api/subtasks/${subtaskId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ status: status })
        });
        
        if (response.ok) {
            const item = checkbox.closest('.subtask-item');
            if (checkbox.checked) {
                item.classList.add('completed');
            } else {
                item.classList.remove('completed');
            }
        }
    } catch (error) {
        console.error('Error updating subtask:', error);
        checkbox.checked = !checkbox.checked;
    }
}

// Add comment
async function addComment(taskId, content) {
    try {
        const response = await fetch('/api/comments', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                taskId: taskId,
                content: content
            })
        });
        
        if (response.ok) {
            location.reload();
        }
    } catch (error) {
        console.error('Error adding comment:', error);
    }
}

// Filter tasks
function filterTasks() {
    const status = document.getElementById('filterStatus').value;
    const priority = document.getElementById('filterPriority').value;
    
    let url = '/tasks?';
    if (status) url += `status=${status}&`;
    if (priority) url += `priority=${priority}&`;
    
    window.location.href = url;
}

// Clear filters
function clearFilters() {
    window.location.href = '/tasks';
}
