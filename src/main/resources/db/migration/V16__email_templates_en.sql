-- ============================================================
-- V16: English versions of the email templates (V1 only had French ones).
-- Emails are sent in the recipient's language, falling back to English.
-- ============================================================

INSERT INTO email_templates (template_key, subject, body_html, body_text, locale) VALUES
('welcome',
 'Welcome to AploseERP',
 '<p>Hello {{userName}},</p><p>Your company <strong>{{companyName}}</strong> has been created on AploseERP.</p><p>Sign in here: <a href="{{loginUrl}}">{{loginUrl}}</a></p><p>See you soon,<br/>The AploseERP team</p>',
 'Hello {{userName}}, Your company {{companyName}} has been created on AploseERP. Sign in here: {{loginUrl}} See you soon, The AploseERP team',
 'en'),
('onboarding',
 'Getting started with AploseERP',
 '<p>Hello {{userName}},</p><p>A few steps to get started with AploseERP:</p><ul><li>Complete your company settings (Settings)</li><li>Add your first customers and contacts (CRM)</li><li>Create a proposal or an invoice (Sales)</li></ul><p><a href="{{loginUrl}}">Go to my workspace</a></p><p>The AploseERP team</p>',
 'Hello {{userName}}, Get started with AploseERP: complete your settings, add your customers and create your first proposals. Go to my workspace: {{loginUrl}}',
 'en'),
('leave.request.submitted',
 'Leave request to approve - {{requesterName}}',
 '<p>Hello {{validatorName}},</p><p><strong>{{requesterName}}</strong> has submitted a leave request.</p><p>Type: {{leaveTypeLabel}}, from {{dateStart}} to {{dateEnd}}.</p><p><a href="{{requestUrl}}">View and approve the request</a></p><p>The AploseERP team</p>',
 'Hello {{validatorName}}, {{requesterName}} has submitted a leave request ({{leaveTypeLabel}}, {{dateStart}} - {{dateEnd}}). View: {{requestUrl}}',
 'en'),
('leave.request.denied',
 'Your leave request has been denied',
 '<p>Hello {{requesterName}},</p><p>Your leave request has been denied.</p><p>Approver''s comment: {{responseComment}}</p><p><a href="{{requestUrl}}">View the request</a></p><p>The AploseERP team</p>',
 'Hello {{requesterName}}, your leave request has been denied. Comment: {{responseComment}}. View: {{requestUrl}}',
 'en');
