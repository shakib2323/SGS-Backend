# SGS API reference

Base URL: `https://api.yourdomain.com`. JSON everywhere except uploads (`multipart/form-data`).
Auth = HttpOnly cookie `SGS_AUTH`. Every **write** (POST/PUT/PATCH/DELETE) must also send the header
`X-Requested-With: XMLHttpRequest`, and `fetch` must use `credentials: 'include'`.
Errors: `{ timestamp, status, code, message, fieldErrors? }`.

## Auth
| Method | Path | Who | Notes |
|---|---|---|---|
| POST | /api/auth/login | public | `{username,password}` -> user info + cookie. 5 failures / 15 min per IP+username, then 429 |
| POST | /api/auth/logout | any | clears cookie |
| GET | /api/auth/me | logged in | role, `mustChangePassword`, `canUploadImages`, `uploadPermissionUntil` |
| POST | /api/auth/change-password | logged in | `{currentPassword,newPassword}`; signs out other sessions. Required first if `mustChangePassword` (otherwise 403 `PASSWORD_CHANGE_REQUIRED`) |

## Public (no login, cached 60 s)
| GET | Path | Query |
|---|---|---|
| | /api/public/categories | |
| | /api/public/gallery | `category` (slug), `q` (title), `page`, `size` |
| | /api/public/gallery/{id} | |
| | /api/public/tools | |
| | /api/public/services, /api/public/services/{slug} | images are ordered for the carousel |
| | /api/public/projects | `category`, `q` (title/client/location), `status`, `featured`, `page`, `size` |
| | /api/public/projects/{id} | |
| | /api/public/clients | logos, ordered |
| | /api/public/contact | list of `{type,label,value}`; types PHONE, EMAIL, ADDRESS, WHATSAPP, TELEGRAM, SMS, WORKING_HOURS, MAP_URL |
| POST | /api/public/inquiries | `{name,email?,phone?,serviceInterest?,message,website(honeypot, leave empty),turnstileToken}`; 5/hour per IP |

## Admin (role ADMIN)
All under `/api/admin/...`. Create endpoints for image-based items are `multipart/form-data`; update endpoints are JSON;
images are replaced with `PUT .../{id}/image` (or `/logo` for clients).

| Resource | Endpoints |
|---|---|
| categories | GET, POST, PUT /{id}, DELETE /{id} (blocked while in use) |
| gallery | GET (adds `uploadedBy`), PUT /{id}, PUT /{id}/image, DELETE /{id}. **Create: `POST /api/gallery`** (admin, or employee with active upload permission). Fields: `title`, `description` (<=10,000 chars), `socialLink` (http/https only), `categoryId`, `image` |
| tools | GET, POST (`title,description,toolLink,displayOrder,active,image`), PUT, PUT /{id}/image, DELETE |
| services | GET, GET /{id}, POST, PUT /{id}, DELETE /{id}; `POST /{id}/images` (field `images`, repeatable, **max 8 per service**), `DELETE /{id}/images/{imageId}`, `PUT /{id}/images/order` `{imageIds:[...]}` |
| projects | POST (`title,description,categoryId,location,clientName,startedYear,completedYear,status,areaSqFt,featured,image`), PUT, PUT /{id}/image, DELETE |
| clients | GET, POST (`name,websiteUrl,displayOrder,active,logo`), PUT, PUT /{id}/logo, DELETE |
| contact | GET, POST, PUT /{id}, DELETE /{id} |
| inquiries | GET `?status=&page=`, PATCH /{id}/status `{status}`, DELETE /{id} |
| employees | see below |
| bank | see below |
| audit | GET `/api/admin/audit?entityType=&page=` |
| dashboard | GET `/api/admin/dashboard` counts |

### Employees (`/api/admin/employees`)
| Method | Path | Body |
|---|---|---|
| POST | / | `{username, fullName, phone?, password?, mustChangePassword?}`; no password -> generated and returned **once** in `generatedPassword` |
| GET | /?q=&state= | state: ACTIVE, SUSPENDED_TEMPORARY, SUSPENDED_PERMANENT |
| PUT | /{id} | `{fullName, phone}` |
| POST | /{id}/suspend | `{type:"TEMPORARY", days:7}` or `{type:"TEMPORARY", untilDate:"2026-12-31"}` or `{type:"PERMANENT"}`, optional `reason`. Signs the employee out immediately |
| POST | /{id}/unsuspend | undo any suspension |
| POST | /reset-password | `{username, newPassword?}` **username is mandatory**; forces password change at next login |
| POST | /{id}/upload-permission | `{hours:24}` or `{untilDate:"2026-11-05"}` |
| DELETE | /{id}/upload-permission | withdraw permission |

### Bank, employee side (`/api/employee/bank`, role EMPLOYEE only)
| Method | Path | Notes |
|---|---|---|
| POST | / | `{amount, note?}`; amount > 0, max 2 decimals; date/time set by the server. Identical amount within 20 s is rejected as a double-submit |
| GET | /?from=&to=&page= | own entries only |
| GET | /summary?from=&to= | `grandTotal` (all time), `rangeTotal`, `days[]` = date-wise totals |
There is no edit or delete endpoint for employees.

### Bank, admin side (`/api/admin/bank`)
| Method | Path | Notes |
|---|---|---|
| GET | / | `employeeId, from, to, includeDeleted, page, size` |
| GET | /overview | per-employee totals + date-wise totals |
| GET | /employees/{id}/summary | works for suspended employees too |
| PUT | /{id} | `{amount, note, reason}` (reason mandatory, history recorded) |
| DELETE | /{id}?reason= | soft delete (removed from totals; stays in history) |
| POST | /{id}/restore?reason= | |
| GET | /{id}/history | every edit/delete/restore: who, when, old/new values, reason |

## Frontend snippets
```js
const API = 'https://api.yourdomain.com';
const api = (path, opts = {}) => fetch(API + path, {
  credentials: 'include',
  headers: { 'X-Requested-With': 'XMLHttpRequest', ...(opts.json ? { 'Content-Type': 'application/json' } : {}) },
  ...opts,
  body: opts.json ? JSON.stringify(opts.json) : opts.body,
});

// login
await api('/api/auth/login', { method: 'POST', json: { username, password } });

// gallery upload (FormData: do NOT set Content-Type yourself)
const fd = new FormData();
fd.append('title', title); fd.append('description', desc);
fd.append('socialLink', link); fd.append('categoryId', categoryId); fd.append('image', file);
await api('/api/gallery', { method: 'POST', body: fd });
```
Always render API text with `textContent` (never `innerHTML`).
