# Admin routes verification checklist

Verify each route while logged in as an admin user (e.g. `admin` / `Admin1234!` on demo tenant).

## Core

| Route | Expected | Result |
|-------|----------|--------|
| GET /dashboard | 200, dashboard view | |
| GET /login | 200, login form (or redirect if already logged in) | |

## CRM

| Route | Expected | Result |
|-------|----------|--------|
| GET /third-parties | 200, list | |
| GET /third-parties/new | 200, form | |
| GET /third-parties/{id} | 200, detail | |
| GET /third-parties/{id}/edit | 200, form | |
| GET /contacts | 200, list | |
| GET /contacts/new | 200, form | |
| GET /contacts/{id} | 200, detail | |
| GET /contacts/{id}/edit | 200, form | |

## Commerce

| Route | Expected | Result |
|-------|----------|--------|
| GET /proposals | 200, list | |
| GET /proposals/new | 200, form | |
| GET /proposals/{id} | 200, detail | |
| GET /proposals/{id}/edit | 200, form | |
| GET /orders | 200, list | |
| GET /orders/new | 200, form | |
| GET /orders/{id} | 200, detail | |
| GET /orders/{id}/edit | 200, form | |
| GET /invoices | 200, list | |
| GET /invoices/new | 200, form | |
| GET /invoices/{id} | 200, detail | |
| GET /invoices/{id}/edit | 200, form | |

## Catalog

| Route | Expected | Result |
|-------|----------|--------|
| GET /products | 200, list | |
| GET /products/categories | 200, list (link from products page) | |
| GET /products/categories/new | 200, form | |
| GET /products/categories/{id}/edit | 200, form | |
| GET /products/new | 200, form | |
| GET /products/{id} | 200, detail | |
| GET /products/{id}/edit | 200, form | |
| GET /currencies | 200, list | |

## Projects & Agenda

| Route | Expected | Result |
|-------|----------|--------|
| GET /projects | 200, list | |
| GET /projects/new | 200, form | |
| GET /projects/{id} | 200, detail | |
| GET /projects/{id}/edit | 200, form | |
| GET /agenda | 200, agenda view | |
| GET /agenda/new | 200, form | |
| GET /agenda/{id} | 200, detail | |
| GET /agenda/{id}/edit | 200, form | |

## Admin

| Route | Expected | Result |
|-------|----------|--------|
| GET /admin/users | 200, list | |
| GET /admin/users/new | 200, form | |
| GET /admin/users/{id}/edit | 200, form | |
| GET /admin/users/{id}/password | 200, password form | |
| GET /admin/roles | 200, list | |
| GET /admin/roles/new | 200, form | |
| GET /admin/roles/{id}/edit | 200, form | |
| GET /admin/settings | 200, tenant settings form | |
| GET /admin/email-templates | 200, list (SUPER_ADMIN only) | |
| GET /admin/email-templates/{id}/edit | 200, form (SUPER_ADMIN only) | |

## Profile

| Route | Expected | Result |
|-------|----------|--------|
| GET /profile | 200, profile form | |
| GET /profile/password | 200, password form | |

---

## Verification note

- **Sidebar links** in `layout/base.html` have been cross-checked: all `th:href` point to the routes above (dashboard, third-parties, contacts, proposals, orders, invoices, products, currencies, projects, agenda, admin/users, admin/roles, admin/settings, admin/email-templates, profile).
- **Controllers** exist for every listed path; list/detail/edit/new mappings match this table.
- **Product categories**: no sidebar entry; access via Products page link to `/products/categories`.
- **Email templates**: only visible in sidebar for users with `ROLE_SUPER_ADMIN` or `SUPER_ADMIN`; demo user `admin` / `Admin1234!` on tenant Aplose has SUPER_ADMIN.

*Fill "Result" column after manual browser checks (e.g. 200, 404, 403) when logged in as admin.*
