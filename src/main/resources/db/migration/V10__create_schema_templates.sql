CREATE TABLE schema_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,

    schema_json JSONB NOT NULL,

    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,

    system_template BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Unique per organization
CREATE UNIQUE INDEX schema_templates_org_slug_unique
ON schema_templates (organization_id, slug)
WHERE organization_id IS NOT NULL;

-- Unique for system templates
CREATE UNIQUE INDEX schema_templates_system_slug_unique
ON schema_templates (slug)
WHERE system_template = true;


INSERT INTO schema_templates (name, slug, description, schema_json, system_template)
VALUES (
'User Profile',
'user-profile',
'Basic user profile schema',
'{
  "id": "uuid",
  "name": "string",
  "email": "email",
  "age": "number",
  "isActive": "boolean",
  "createdAt": "datetime"
}',
true
);


INSERT INTO schema_templates VALUES (
gen_random_uuid(),
'Login Response',
'login-response',
'Authentication API response',
'{
  "userId": "uuid",
  "token": "string",
  "expiresAt": "datetime",
  "role": { "type": "enum", "values": ["ADMIN", "USER"] }
}',
NULL,
true,
NOW(),
NOW()
);


INSERT INTO schema_templates VALUES (
gen_random_uuid(),
'Product Item',
'product-item',
'Ecommerce product schema',
'{
  "id": "uuid",
  "name": "string",
  "price": "number",
  "currency": { "type": "enum", "values": ["USD", "EUR", "INR"] },
  "inStock": "boolean"
}',
NULL,
true,
NOW(),
NOW()
);


INSERT INTO schema_templates VALUES (
gen_random_uuid(),
'Order Summary',
'order-summary',
'Order API response',
'{
  "orderId": "uuid",
  "totalAmount": "number",
  "status": { "type": "enum", "values": ["PENDING", "PAID", "SHIPPED", "CANCELLED"] },
  "createdAt": "datetime"
}',
NULL,
true,
NOW(),
NOW()
);


INSERT INTO schema_templates VALUES (
gen_random_uuid(),
'Blog Post',
'blog-post',
'Blog post API data',
'{
  "id": "uuid",
  "title": "string",
  "content": "string",
  "authorEmail": "email",
  "published": "boolean",
  "publishedAt": "datetime"
}',
NULL,
true,
NOW(),
NOW()
);

INSERT INTO schema_templates VALUES (
gen_random_uuid(),
'Comment',
'comment',
'User comment schema',
'{
  "id": "uuid",
  "postId": "uuid",
  "message": "string",
  "author": "string",
  "createdAt": "datetime"
}',
NULL,
true,
NOW(),
NOW()
);

INSERT INTO schema_templates VALUES (
gen_random_uuid(),
'Payment Transaction',
'payment-transaction',
'Payment gateway response',
'{
  "transactionId": "uuid",
  "amount": "number",
  "method": { "type": "enum", "values": ["CARD", "UPI", "NET_BANKING"] },
  "status": { "type": "enum", "values": ["SUCCESS", "FAILED", "PENDING"] },
  "timestamp": "datetime"
}',
NULL,
true,
NOW(),
NOW()
);

INSERT INTO schema_templates VALUES (
gen_random_uuid(),
'Notification',
'notification',
'User notification object',
'{
  "id": "uuid",
  "title": "string",
  "message": "string",
  "read": "boolean",
  "createdAt": "datetime"
}',
NULL,
true,
NOW(),
NOW()
);


INSERT INTO schema_templates VALUES (
gen_random_uuid(),
'File Metadata',
'file-metadata',
'Uploaded file information',
'{
  "fileId": "uuid",
  "fileName": "string",
  "size": "number",
  "mimeType": "string",
  "uploadedAt": "datetime"
}',
NULL,
true,
NOW(),
NOW()
);


INSERT INTO schema_templates VALUES (
gen_random_uuid(),
'Dashboard Stats',
'dashboard-stats',
'Admin dashboard metrics',
'{
  "totalUsers": "number",
  "activeUsers": "number",
  "totalOrders": "number",
  "revenue": "number",
  "generatedAt": "datetime"
}',
NULL,
true,
NOW(),
NOW()
);


