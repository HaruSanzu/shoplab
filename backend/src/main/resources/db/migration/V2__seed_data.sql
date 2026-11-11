-- V2: test data. Same products and users as the frontend (frontend/index.html),
-- so UI tests and API tests can rely on identical, known data.

INSERT INTO products (id, sku, title, brand, category, description, price, rating, reviews_count, stock, emoji, color) VALUES
 (1, 'SL-00001',  'Wireless Headphones X200',  'Sonora',    'Electronics',  '',  79.99, 4.5, 1284,  15, '🎧', '#e3edf7'),
 (2, 'SL-00002',  'Smartwatch Pulse 3',        'Tikko',     'Electronics',  '', 149.99, 4.2,  842,   8, '⌚', '#ece6f5'),
 (3, 'SL-00003',  '4K Action Camera',          'Vistra',    'Electronics',  '', 199.00, 4.0,  311,   0, '📷', '#e8ecef'),
 (4, 'SL-00004',  'Bluetooth Speaker Mini',    'Sonora',    'Electronics',  '',  34.99, 4.6, 2210,  40, '🔊', '#e2f3ef'),
 (5, 'SL-00005',  'Mechanical Keyboard K87',   'Keyforge',  'Electronics',  '',  89.50, 4.7,  978,  12, '⌨️', '#f1ece4'),
 (6, 'SL-00006',  'USB-C Charger 65W',         'Voltix',    'Electronics',  '',  29.99, 4.4, 3120, 100, '🔌', '#f6f0dc'),
 (7, 'SL-00007',  'Selenium in Practice',      'Bytepress', 'Books',        '',  39.99, 4.8,  156,  25, '📗', '#e3f2e1'),
 (8, 'SL-00008',  'Learning Java Step by Step','Bytepress', 'Books',        '',  24.99, 4.3,  402,  30, '📘', '#e0ebf8'),
 (9, 'SL-00009',  'BDD with Gherkin',          'Bytepress', 'Books',        '',  19.99, 4.1,   88,  18, '📙', '#faeadb'),
 (10, 'SL-00010', 'Mystery at Harbor Lane',    'Inkwell',   'Books',        '',  12.99, 3.9,  640,   3, '📕', '#f7e1e1'),
 (11, 'SL-00011', 'The Debugging Mindset',     'Inkwell',   'Books',        '',  27.50, 4.4,  204,   9, '📓', '#ecebe6'),
 (12, 'SL-00012', 'Coffee Maker Brew 12',      'Hearth',    'Home',         '',  59.99, 4.3, 1530,  20, '☕', '#efe5dc'),
 (13, 'SL-00013', 'LED Desk Lamp',             'Lumo',      'Home',         '',  22.49, 4.5,  719,  35, '💡', '#fbf3d5'),
 (14, 'SL-00014', 'Ceramic Mug Set (4 pcs)',   'Hearth',    'Home',         '',  18.00, 4.6,  388,  50, '🍵', '#e4f0ea'),
 (15, 'SL-00015', 'Robot Vacuum R5',           'Cleanbot',  'Home',         '', 249.99, 4.1,  960,   2, '🤖', '#e6e9ee'),
 (16, 'SL-00016', 'Yoga Mat Pro',              'Flexa',     'Sports',       '',  27.99, 4.4, 1102,  45, '🧘', '#e9e3f4'),
 (17, 'SL-00017', 'Running Shoes Velo',        'Stride',    'Sports',       '',  94.99, 4.5, 2045,  14, '👟', '#e1eef7'),
 (18, 'SL-00018', 'Adjustable Dumbbells 20kg', 'Ironix',    'Sports',       '', 129.00, 4.6,  530,   0, '🏋️', '#eceff1'),
 (19, 'SL-00019', 'Water Bottle 1L',           'Flexa',     'Sports',       '',  14.99, 4.2,  870,  60, '🧴', '#ddf1f6'),
 (20, 'SL-00020', 'Building Blocks 500 pcs',   'Brixo',     'Toys',         '',  39.99, 4.7, 1320,  22, '🧱', '#fbe4dc'),
 (21, 'SL-00021', 'Remote Control Car',        'Zoomer',    'Toys',         '',  54.99, 4.0,  415,  11, '🏎️', '#fde8e8'),
 (22, 'SL-00022', 'Jigsaw Puzzle 1000',        'Brixo',     'Toys',         '',  16.99, 4.3,  299,  16, '🧩', '#e3f1e6'),
 (23, 'SL-00023', 'Leather Wallet',            'Northway',  'Fashion',      '',  32.00, 4.4,  690,  27, '👛', '#f1e7de'),
 (24, 'SL-00024', 'Classic Sunglasses',        'Northway',  'Fashion',      '',  45.00, 3.8,  233,  19, '🕶️', '#e7e9ec'),
 (25, 'SL-00025', 'Winter Hoodie',             'Stride',    'Fashion',      '',  49.99, 4.5,  812,   7, '🧥', '#e2e7f3');

-- The description is built from other columns, exactly like the frontend does.
UPDATE products SET
    description =title || ' by ' || brand || ' is one of the most popular items in our ' || category
               || ' department. Built for everyday use, it combines solid quality with a fair price.'
               || ' Covered by a 30-day return policy and a 1-year warranty.';

-- We inserted ids by hand, so move the id counter past them (next new product gets id 26).
SELECT setval('products_id_seq', (SELECT max(id) FROM products));

-- Built-in test users, all with password "Password123".
-- pgcrypto's crypt(..., gen_salt('bf')) produces a BCrypt hash, the same format
-- Java's BCryptPasswordEncoder checks at login.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (email, password_hash, name, locked, slow_login) VALUES
 ('standard@shoplab.test', crypt('Password123', gen_salt('bf', 10)), 'Standard User', FALSE, FALSE),
 ('locked@shoplab.test',   crypt('Password123', gen_salt('bf', 10)), 'Locked User',   TRUE,  FALSE),
 ('slow@shoplab.test',     crypt('Password123', gen_salt('bf', 10)), 'Slow User',     FALSE, TRUE);
