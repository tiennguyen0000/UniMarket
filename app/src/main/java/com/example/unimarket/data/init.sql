-- =========================================================
-- SUPABASE ONE-RUN INIT SCRIPT (UUID VERSION)
-- init schema + sample data + permissions for testing
-- =========================================================

begin;

create extension if not exists pgcrypto;

-- =========================================================
-- 0) CLEAN UP
-- =========================================================

drop table if exists public.reports cascade;
drop table if exists public.notifications cascade;
drop table if exists public.user_behavior cascade;
drop table if exists public.wishlist cascade;
drop table if exists public.reviews cascade;
drop table if exists public.messages cascade;
drop table if exists public.conversation_participants cascade;
drop table if exists public.conversations cascade;
drop table if exists public.order_items cascade;
drop table if exists public.orders cascade;
drop table if exists public.cart_items cascade;
drop table if exists public.carts cascade;
drop table if exists public.product_images cascade;
drop table if exists public.products cascade;
drop table if exists public.categories cascade;
drop table if exists public.student_verifications cascade;
drop table if exists public.users cascade;

drop function if exists public.set_updated_at() cascade;

-- =========================================================
-- 1) TABLES
-- =========================================================

create table public.users (
    id uuid primary key default gen_random_uuid(),
    email varchar(255) unique not null,
    password_hash varchar(255) not null,
    full_name varchar(255),
    phone varchar(20),
    university varchar(255),
    avatar_url text,
    is_verified boolean default false,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table public.student_verifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references public.users(id) on delete cascade,
    method varchar(50),
    status varchar(50),
    proof_url text,
    created_at timestamp default current_timestamp
);

create table public.categories (
    id uuid primary key default gen_random_uuid(),
    name varchar(255) not null,
    parent_id uuid references public.categories(id) on delete set null
);

create table public.products (
    id uuid primary key default gen_random_uuid(),
    seller_id uuid references public.users(id) on delete cascade,
    title varchar(255) not null,
    description text,
    price decimal(10,2) not null,
    category_id uuid references public.categories(id),
    condition varchar(50),
    status varchar(50) default 'active',
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table public.product_images (
    id uuid primary key default gen_random_uuid(),
    product_id uuid references public.products(id) on delete cascade,
    image_url text not null
);

create table public.carts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references public.users(id) on delete cascade,
    created_at timestamp default current_timestamp
);

create table public.cart_items (
    id uuid primary key default gen_random_uuid(),
    cart_id uuid references public.carts(id) on delete cascade,
    product_id uuid references public.products(id),
    quantity int default 1
);

create table public.orders (
    id uuid primary key default gen_random_uuid(),
    buyer_id uuid references public.users(id),
    total_price decimal(10,2),
    status varchar(50) default 'pending',
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table public.order_items (
    id uuid primary key default gen_random_uuid(),
    order_id uuid references public.orders(id) on delete cascade,
    product_id uuid references public.products(id),
    seller_id uuid references public.users(id),
    price decimal(10,2)
);

create table public.conversations (
    id uuid primary key default gen_random_uuid(),
    created_at timestamp default current_timestamp
);

create table public.conversation_participants (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid references public.conversations(id) on delete cascade,
    user_id uuid references public.users(id)
);

create table public.messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid references public.conversations(id) on delete cascade,
    sender_id uuid references public.users(id),
    content text,
    created_at timestamp default current_timestamp
);

create table public.reviews (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references public.users(id),
    product_id uuid references public.products(id),
    rating int check (rating >= 1 and rating <= 5),
    comment text,
    created_at timestamp default current_timestamp
);

create table public.wishlist (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references public.users(id) on delete cascade,
    product_id uuid references public.products(id) on delete cascade,
    created_at timestamp default current_timestamp,
    unique (user_id, product_id)
);

create table public.user_behavior (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references public.users(id) on delete cascade,
    product_id uuid references public.products(id),
    action varchar(50),
    created_at timestamp default current_timestamp
);

create table public.notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references public.users(id) on delete cascade,
    content text,
    is_read boolean default false,
    created_at timestamp default current_timestamp
);

create table public.reports (
    id uuid primary key default gen_random_uuid(),
    reporter_id uuid references public.users(id),
    product_id uuid references public.products(id),
    reason text,
    created_at timestamp default current_timestamp
);

-- =========================================================
-- 2) INDEXES
-- =========================================================

create index idx_student_verifications_user_id on public.student_verifications(user_id);
create index idx_categories_parent_id on public.categories(parent_id);
create index idx_products_seller_id on public.products(seller_id);
create index idx_products_category_id on public.products(category_id);
create index idx_products_status on public.products(status);
create index idx_product_images_product_id on public.product_images(product_id);
create index idx_carts_user_id on public.carts(user_id);
create index idx_cart_items_cart_id on public.cart_items(cart_id);
create index idx_cart_items_product_id on public.cart_items(product_id);
create index idx_orders_buyer_id on public.orders(buyer_id);
create index idx_orders_status on public.orders(status);
create index idx_order_items_order_id on public.order_items(order_id);
create index idx_order_items_product_id on public.order_items(product_id);
create index idx_order_items_seller_id on public.order_items(seller_id);
create index idx_conversation_participants_conversation_id on public.conversation_participants(conversation_id);
create index idx_conversation_participants_user_id on public.conversation_participants(user_id);
create index idx_messages_conversation_id on public.messages(conversation_id);
create index idx_messages_sender_id on public.messages(sender_id);
create index idx_reviews_user_id on public.reviews(user_id);
create index idx_reviews_product_id on public.reviews(product_id);
create index idx_user_behavior_user_id on public.user_behavior(user_id);
create index idx_user_behavior_product_id on public.user_behavior(product_id);
create index idx_notifications_user_id on public.notifications(user_id);
create index idx_reports_reporter_id on public.reports(reporter_id);
create index idx_reports_product_id on public.reports(product_id);

-- =========================================================
-- 3) UPDATED_AT TRIGGER
-- =========================================================

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = current_timestamp;
  return new;
end;
$$;

create trigger trg_users_updated_at
before update on public.users
for each row
execute function public.set_updated_at();

create trigger trg_products_updated_at
before update on public.products
for each row
execute function public.set_updated_at();

create trigger trg_orders_updated_at
before update on public.orders
for each row
execute function public.set_updated_at();

-- =========================================================
-- 4) SAMPLE DATA
-- =========================================================

-- USERS
insert into public.users (email, password_hash, full_name, phone, university, avatar_url, is_verified)
values
(
    'an@example.com',
    'hash_123',
    'Nguyen Van An',
    '0900000001',
    'UIT',
    'https://xdcjvfrjgwfsrezivpqo.supabase.co/storage/v1/object/public/users/1.png',
    true
),
(
    'binh@example.com',
    'hash_123',
    'Tran Thi Binh',
    '0900000002',
    'HCMUS',
    'https://xdcjvfrjgwfsrezivpqo.supabase.co/storage/v1/object/public/users/2.png',
    true
),
(
    'cuong@example.com',
    'hash_123',
    'Le Minh Cuong',
    '0900000003',
    'BKU',
    'https://xdcjvfrjgwfsrezivpqo.supabase.co/storage/v1/object/public/users/3.png',
    false
),
(
    'dung@example.com',
    'hash_123',
    'Pham Quoc Dung',
    '0900000004',
    'UIT',
    'https://xdcjvfrjgwfsrezivpqo.supabase.co/storage/v1/object/public/users/4.png',
    true
);

-- STUDENT VERIFICATIONS
insert into public.student_verifications (user_id, method, status, proof_url)
values
(
    (select id from public.users where email = 'an@example.com'),
    'email',
    'approved',
    'https://example.com/proof1.pdf'
),
(
    (select id from public.users where email = 'binh@example.com'),
    'student_card',
    'approved',
    'https://example.com/proof2.jpg'
),
(
    (select id from public.users where email = 'cuong@example.com'),
    'email',
    'pending',
    'https://example.com/proof3.pdf'
);

-- CATEGORIES
insert into public.categories (name, parent_id)
values
('Electronics', null),
('Books', null),
('Fashion', null);

insert into public.categories (name, parent_id)
values
('Phones', (select id from public.categories where name = 'Electronics')),
('Laptops', (select id from public.categories where name = 'Electronics')),
('Textbooks', (select id from public.categories where name = 'Books'));

-- PRODUCTS
insert into public.products (seller_id, title, description, price, category_id, condition, status)
values
(
    (select id from public.users where email = 'an@example.com'),
    'iPhone 12 64GB',
    'Used, good condition',
    9500000,
    (select id from public.categories where name = 'Phones'),
    'used',
    'active'
),
(
    (select id from public.users where email = 'binh@example.com'),
    'MacBook Air M1',
    'Like new, full box',
    16500000,
    (select id from public.categories where name = 'Laptops'),
    'used',
    'active'
),
(
    (select id from public.users where email = 'an@example.com'),
    'Data Structures Textbook',
    'Second-hand textbook for students',
    120000,
    (select id from public.categories where name = 'Textbooks'),
    'used',
    'active'
),
(
    (select id from public.users where email = 'dung@example.com'),
    'Uniqlo Jacket',
    'Black jacket size M',
    350000,
    (select id from public.categories where name = 'Fashion'),
    'used',
    'active'
);

-- PRODUCT IMAGES
-- mapping đúng như bạn muốn:
-- product 1 -> 1v1, 1v2
-- product 2 -> 2
-- product 3 -> 3
-- product 4 -> 4

insert into public.product_images (product_id, image_url)
values
(
    (select id from public.products where title = 'iPhone 12 64GB'),
    'https://xdcjvfrjgwfsrezivpqo.supabase.co/storage/v1/object/public/products/1v1.png'
),
(
    (select id from public.products where title = 'iPhone 12 64GB'),
    'https://xdcjvfrjgwfsrezivpqo.supabase.co/storage/v1/object/public/products/1v2.png'
),
(
    (select id from public.products where title = 'MacBook Air M1'),
    'https://xdcjvfrjgwfsrezivpqo.supabase.co/storage/v1/object/public/products/2.png'
),
(
    (select id from public.products where title = 'Data Structures Textbook'),
    'https://xdcjvfrjgwfsrezivpqo.supabase.co/storage/v1/object/public/products/3.png'
),
(
    (select id from public.products where title = 'Uniqlo Jacket'),
    'https://xdcjvfrjgwfsrezivpqo.supabase.co/storage/v1/object/public/products/4.png'
);

-- CARTS
insert into public.carts (user_id)
values
((select id from public.users where email = 'binh@example.com')),
((select id from public.users where email = 'cuong@example.com'));

-- CART ITEMS
insert into public.cart_items (cart_id, product_id, quantity)
values
(
    (select c.id
     from public.carts c
     join public.users u on u.id = c.user_id
     where u.email = 'binh@example.com'),
    (select id from public.products where title = 'iPhone 12 64GB'),
    1
),
(
    (select c.id
     from public.carts c
     join public.users u on u.id = c.user_id
     where u.email = 'binh@example.com'),
    (select id from public.products where title = 'Data Structures Textbook'),
    2
),
(
    (select c.id
     from public.carts c
     join public.users u on u.id = c.user_id
     where u.email = 'cuong@example.com'),
    (select id from public.products where title = 'Uniqlo Jacket'),
    1
);

-- ORDERS
insert into public.orders (buyer_id, total_price, status)
values
(
    (select id from public.users where email = 'binh@example.com'),
    9620000,
    'pending'
),
(
    (select id from public.users where email = 'cuong@example.com'),
    350000,
    'accepted'
);

-- ORDER ITEMS
insert into public.order_items (order_id, product_id, seller_id, price)
values
(
    (select id from public.orders where buyer_id = (select id from public.users where email = 'binh@example.com') and status = 'pending'),
    (select id from public.products where title = 'iPhone 12 64GB'),
    (select id from public.users where email = 'an@example.com'),
    9500000
),
(
    (select id from public.orders where buyer_id = (select id from public.users where email = 'binh@example.com') and status = 'pending'),
    (select id from public.products where title = 'Data Structures Textbook'),
    (select id from public.users where email = 'an@example.com'),
    120000
),
(
    (select id from public.orders where buyer_id = (select id from public.users where email = 'cuong@example.com') and status = 'accepted'),
    (select id from public.products where title = 'Uniqlo Jacket'),
    (select id from public.users where email = 'dung@example.com'),
    350000
);

-- CONVERSATIONS
insert into public.conversations default values;

insert into public.conversation_participants (conversation_id, user_id)
values
(
    (select id from public.conversations limit 1),
    (select id from public.users where email = 'an@example.com')
),
(
    (select id from public.conversations limit 1),
    (select id from public.users where email = 'binh@example.com')
);

insert into public.messages (conversation_id, sender_id, content)
values
(
    (select id from public.conversations limit 1),
    (select id from public.users where email = 'an@example.com'),
    'Chao ban, san pham nay con khong?'
),
(
    (select id from public.conversations limit 1),
    (select id from public.users where email = 'binh@example.com'),
    'Con ban nhe, ban muon xem them anh khong?'
);

-- REVIEWS
insert into public.reviews (user_id, product_id, rating, comment)
values
(
    (select id from public.users where email = 'binh@example.com'),
    (select id from public.products where title = 'iPhone 12 64GB'),
    5,
    'San pham tot, nguoi ban nhiet tinh'
),
(
    (select id from public.users where email = 'cuong@example.com'),
    (select id from public.products where title = 'Uniqlo Jacket'),
    4,
    'Ao dep, dung mo ta'
);

-- WISHLIST
insert into public.wishlist (user_id, product_id)
values
(
    (select id from public.users where email = 'binh@example.com'),
    (select id from public.products where title = 'MacBook Air M1')
),
(
    (select id from public.users where email = 'binh@example.com'),
    (select id from public.products where title = 'Uniqlo Jacket')
),
(
    (select id from public.users where email = 'cuong@example.com'),
    (select id from public.products where title = 'iPhone 12 64GB')
);

-- USER BEHAVIOR
insert into public.user_behavior (user_id, product_id, action)
values
(
    (select id from public.users where email = 'binh@example.com'),
    (select id from public.products where title = 'iPhone 12 64GB'),
    'view'
),
(
    (select id from public.users where email = 'binh@example.com'),
    (select id from public.products where title = 'iPhone 12 64GB'),
    'add_to_cart'
),
(
    (select id from public.users where email = 'cuong@example.com'),
    (select id from public.products where title = 'Uniqlo Jacket'),
    'click'
),
(
    (select id from public.users where email = 'cuong@example.com'),
    (select id from public.products where title = 'Uniqlo Jacket'),
    'view'
);

-- NOTIFICATIONS
insert into public.notifications (user_id, content, is_read)
values
(
    (select id from public.users where email = 'an@example.com'),
    'Ban co don hang moi dang cho xu ly',
    false
),
(
    (select id from public.users where email = 'binh@example.com'),
    'San pham trong wishlist vua giam gia',
    false
),
(
    (select id from public.users where email = 'cuong@example.com'),
    'Tai khoan xac minh sinh vien dang cho duyet',
    true
);

-- REPORTS
insert into public.reports (reporter_id, product_id, reason)
values
(
    (select id from public.users where email = 'binh@example.com'),
    (select id from public.products where title = 'iPhone 12 64GB'),
    'Nghi ngo thong tin mo ta chua day du'
),
(
    (select id from public.users where email = 'cuong@example.com'),
    (select id from public.products where title = 'Uniqlo Jacket'),
    'Can kiem tra tinh trang san pham'
);

-- =========================================================
-- 5) GRANTS
-- =========================================================

grant usage on schema public to anon, authenticated;

grant select, insert, update, delete on all tables in schema public to anon, authenticated;

alter default privileges in schema public
grant select, insert, update, delete on tables to anon, authenticated;

-- =========================================================
-- 6) RLS
-- =========================================================

alter table public.users enable row level security;
alter table public.student_verifications enable row level security;
alter table public.categories enable row level security;
alter table public.products enable row level security;
alter table public.product_images enable row level security;
alter table public.carts enable row level security;
alter table public.cart_items enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;
alter table public.conversations enable row level security;
alter table public.conversation_participants enable row level security;
alter table public.messages enable row level security;
alter table public.reviews enable row level security;
alter table public.wishlist enable row level security;
alter table public.user_behavior enable row level security;
alter table public.notifications enable row level security;
alter table public.reports enable row level security;

create policy "test_all_users"
on public.users
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_student_verifications"
on public.student_verifications
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_categories"
on public.categories
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_products"
on public.products
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_product_images"
on public.product_images
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_carts"
on public.carts
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_cart_items"
on public.cart_items
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_orders"
on public.orders
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_order_items"
on public.order_items
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_conversations"
on public.conversations
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_conversation_participants"
on public.conversation_participants
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_messages"
on public.messages
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_reviews"
on public.reviews
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_wishlist"
on public.wishlist
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_user_behavior"
on public.user_behavior
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_notifications"
on public.notifications
for all
to anon, authenticated
using (true)
with check (true);

create policy "test_all_reports"
on public.reports
for all
to anon, authenticated
using (true)
with check (true);

commit;