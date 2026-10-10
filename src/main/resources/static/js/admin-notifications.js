/*
 * Admin Dashboard notifications: bell dropdown, notification section and details modal.
 * Talks only to /admin/api/notifications (ROLE_ADMIN). All server data is inserted with textContent
 * (never innerHTML), so booking/customer values can't inject markup.
 */
(function () {
    'use strict';

    var API = '/admin/api/notifications';
    var POLL_MS = 20000;

    // Visual style per notification type (CSS classes live in admin/dashboard.html)
    var TYPE_META = {
        NEW_BOOKING:       { icon: 'fa-car-side',            cls: 'nt-booking'   },
        PARKING_ACTIVE:    { icon: 'fa-clock',               cls: 'nt-active'    },
        PARKING_COMPLETED: { icon: 'fa-flag-checkered',      cls: 'nt-completed' },
        PAYMENT_COMPLETED: { icon: 'fa-indian-rupee-sign',   cls: 'nt-payment'   },
        BOOKING_CANCELLED: { icon: 'fa-ban',                 cls: 'nt-cancelled' },
        SYSTEM_ERROR:      { icon: 'fa-triangle-exclamation', cls: 'nt-system'   }
    };

    var state = { items: [], unread: 0, selectedId: null };
    var detailsModal = null;

    // ---------------------------------------------------------------- helpers
    function $(id) { return document.getElementById(id); }

    function el(tag, className, text) {
        var node = document.createElement(tag);
        if (className) { node.className = className; }
        if (text !== undefined && text !== null) { node.textContent = text; }
        return node;
    }

    function metaFor(type) { return TYPE_META[type] || TYPE_META.SYSTEM_ERROR; }

    function request(path, method) {
        return fetch(API + path, {
            method: method || 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
        }).then(function (res) {
            if (res.status === 401 || res.status === 403) {
                window.location.href = '/login';
                throw new Error('Not authorised');
            }
            if (!res.ok) { throw new Error('Request failed: ' + res.status); }
            return res.json();
        });
    }

    // ---------------------------------------------------------------- rendering
    function buildItem(n, compact) {
        var meta = metaFor(n.type);
        var row = el('div', 'notif-item ' + meta.cls + (n.read ? ' is-read' : ' is-unread'));
        row.setAttribute('role', 'button');
        row.setAttribute('tabindex', '0');
        row.setAttribute('data-id', n.id);

        var icon = el('div', 'notif-icon');
        icon.appendChild(el('i', 'fa-solid ' + meta.icon));
        row.appendChild(icon);

        var body = el('div', 'notif-body');
        var head = el('div', 'notif-title');
        head.appendChild(el('span', null, n.title));
        if (!n.read) { head.appendChild(el('span', 'notif-dot')); }
        body.appendChild(head);
        body.appendChild(el('div', 'notif-msg', n.message));
        body.appendChild(el('div', 'notif-time', n.createdAt));

        if (!compact) {
            var actions = el('div', 'notif-actions');
            var view = el('button', 'btn btn-sm btn-outline-info', 'View details');
            view.type = 'button';
            view.addEventListener('click', function (e) { e.stopPropagation(); openDetails(n.id); });
            actions.appendChild(view);
            if (!n.read) {
                var mark = el('button', 'btn btn-sm btn-outline-secondary', 'Mark as read');
                mark.type = 'button';
                mark.addEventListener('click', function (e) { e.stopPropagation(); markRead(n.id); });
                actions.appendChild(mark);
            }
            body.appendChild(actions);
        }
        row.appendChild(body);

        row.addEventListener('click', function () { openDetails(n.id); });
        row.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); openDetails(n.id); }
        });
        return row;
    }

    function fillList(container, items, compact, emptyText) {
        if (!container) { return; }
        container.textContent = '';
        if (!items.length) {
            container.appendChild(el('div', 'notif-empty', emptyText));
            return;
        }
        items.forEach(function (n) { container.appendChild(buildItem(n, compact)); });
    }

    function render() {
        var badge = $('notifBadge');
        if (badge) {
            badge.textContent = state.unread > 99 ? '99+' : String(state.unread);
            badge.classList.toggle('d-none', state.unread === 0);
        }
        var bell = $('notifBell');
        if (bell) { bell.classList.toggle('has-unread', state.unread > 0); }
        var label = $('notifCountLabel');
        if (label) { label.textContent = String(state.unread); }
        var sectionCount = $('notifSectionCount');
        if (sectionCount) {
            sectionCount.textContent = state.unread + ' unread';
            sectionCount.classList.toggle('bg-danger', state.unread > 0);
            sectionCount.classList.toggle('bg-secondary', state.unread === 0);
        }
        fillList($('notifDropdownList'), state.items.slice(0, 8), true, 'No notifications yet.');
        fillList($('notifSectionList'), state.items, false,
            'No notifications yet. New bookings and completed parking sessions will appear here.');
        if (state.selectedId !== null) { refreshModalFooter(); }
    }

    // ---------------------------------------------------------------- details modal
    function findItem(id) {
        for (var i = 0; i < state.items.length; i++) {
            if (state.items[i].id === id) { return state.items[i]; }
        }
        return null;
    }

    function openDetails(id) {
        var n = findItem(id);
        if (!n) { return; }
        state.selectedId = id;
        var meta = metaFor(n.type);

        var titleBox = $('notifModalTitle');
        titleBox.textContent = '';
        var icon = el('span', 'notif-icon me-2 ' + meta.cls);
        icon.appendChild(el('i', 'fa-solid ' + meta.icon));
        titleBox.appendChild(icon);
        titleBox.appendChild(el('span', null, n.title));

        $('notifModalTime').textContent = n.createdAt + (n.read && n.readAt ? '  \u2022  read ' + n.readAt : '');

        var body = $('notifModalBody');
        body.textContent = '';
        var sections = n.details || {};
        Object.keys(sections).forEach(function (heading) {
            body.appendChild(el('h6', 'notif-section-heading', heading));
            var table = el('table', 'table table-sm table-borderless notif-detail-table mb-2');
            var tbody = el('tbody');
            var fields = sections[heading];
            Object.keys(fields).forEach(function (label) {
                var tr = el('tr');
                tr.appendChild(el('td', 'notif-detail-label', label));
                tr.appendChild(el('td', 'notif-detail-value', fields[label]));
                tbody.appendChild(tr);
            });
            table.appendChild(tbody);
            body.appendChild(table);
        });
        if (!Object.keys(sections).length) {
            body.appendChild(el('p', 'text-secondary mb-0', n.message));
        }

        refreshModalFooter();
        if (!detailsModal) { detailsModal = new bootstrap.Modal($('notifDetailsModal')); }
        detailsModal.show();
    }

    function refreshModalFooter() {
        var n = findItem(state.selectedId);
        var btn = $('notifModalMarkRead');
        if (btn && n) { btn.classList.toggle('d-none', n.read); }
    }

    // ---------------------------------------------------------------- actions
    function applyList(data) {
        state.items = data.notifications || [];
        state.unread = data.unreadCount || 0;
        render();
    }

    function refresh() {
        return request('?limit=20').then(applyList).catch(function (e) {
            if (window.console) { console.warn('Notification refresh failed:', e.message); }
        });
    }

    function markRead(id) {
        return request('/' + id + '/read', 'POST').then(refresh).catch(function () {});
    }

    function markAllRead() {
        return request('/read-all', 'POST').then(refresh).catch(function () {});
    }

    // ---------------------------------------------------------------- init
    document.addEventListener('DOMContentLoaded', function () {
        ['notifMarkAllBtn', 'notifSectionMarkAll'].forEach(function (id) {
            var b = $(id);
            if (b) { b.addEventListener('click', function (e) { e.preventDefault(); markAllRead(); }); }
        });
        var modalMark = $('notifModalMarkRead');
        if (modalMark) {
            modalMark.addEventListener('click', function () {
                if (state.selectedId !== null) { markRead(state.selectedId); }
            });
        }
        var modalEl = $('notifDetailsModal');
        if (modalEl) {
            modalEl.addEventListener('hidden.bs.modal', function () { state.selectedId = null; });
        }

        refresh();
        setInterval(function () { if (!document.hidden) { refresh(); } }, POLL_MS);
    });
})();
