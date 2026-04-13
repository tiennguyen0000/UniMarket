-- =========================================================
-- SUPABASE ONE-RUN INIT SCRIPT (HYBRID MINIMAL VERSION)
-- Firebase Auth + Supabase DB (Shared IDs)
-- =========================================================

begin;

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
drop table if exists public.profiles cascade;

drop function if exists public.set_updated_at() cascade;

-- =========================================================
-- 1) TABLES
-- =========================================================

-- Bảng profiles dùng chung ID với Firebase UID
create table public.profiles (
    id text primary key, -- Firebase UID
    full_name varchar(255),
    phone varchar(20),
    university varchar(255),
    avatar_url text,
    is_verified boolean default false,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table public.categories (
    id uuid primary key default gen_random_uuid(),
    name varchar(255) not null,
    parent_id uuid references public.categories(id) on delete set null
);

create table public.products (
    id uuid primary key default gen_random_uuid(),
    seller_id text references public.profiles(id) on delete cascade,
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
    user_id text references public.profiles(id) on delete cascade,
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
    buyer_id text references public.profiles(id),
    total_price decimal(10,2),
    status varchar(50) default 'pending',
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table public.order_items (
    id uuid primary key default gen_random_uuid(),
    order_id uuid references public.orders(id) on delete cascade,
    product_id uuid references public.products(id),
    seller_id text references public.profiles(id),
    price decimal(10,2)
);

create table public.conversations (
    id uuid primary key default gen_random_uuid(),
    created_at timestamp default current_timestamp
);

create table public.conversation_participants (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid references public.conversations(id) on delete cascade,
    user_id text references public.profiles(id)
);

create table public.messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid references public.conversations(id) on delete cascade,
    sender_id text references public.profiles(id),
    content text,
    created_at timestamp default current_timestamp
);

create table public.reviews (
    id uuid primary key default gen_random_uuid(),
    user_id text references public.profiles(id),
    product_id uuid references public.products(id),
    rating int check (rating >= 1 and rating <= 5),
    comment text,
    created_at timestamp default current_timestamp
);

create table public.wishlist (
    id uuid primary key default gen_random_uuid(),
    user_id text references public.profiles(id) on delete cascade,
    product_id uuid references public.products(id) on delete cascade,
    created_at timestamp default current_timestamp,
    unique (user_id, product_id)
);

create table public.user_behavior (
    id uuid primary key default gen_random_uuid(),
    user_id text references public.profiles(id) on delete cascade,
    product_id uuid references public.products(id),
    action varchar(50),
    created_at timestamp default current_timestamp
);

create table public.notifications (
    id uuid primary key default gen_random_uuid(),
    user_id text references public.profiles(id) on delete cascade,
    content text,
    is_read boolean default false,
    created_at timestamp default current_timestamp
);

create table public.reports (
    id uuid primary key default gen_random_uuid(),
    reporter_id text references public.profiles(id),
    product_id uuid references public.products(id),
    reason text,
    created_at timestamp default current_timestamp
);

-- =========================================================
-- 2) UPDATED_AT TRIGGER
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

create trigger trg_profiles_updated_at
before update on public.profiles
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
-- 3) PERMISSIONS & RLS
-- =========================================================

grant usage on schema public to anon, authenticated;
grant select, insert, update, delete on all tables in schema public to anon, authenticated;

alter table public.profiles enable row level security;
alter table public.products enable row level security;
alter table public.orders enable row level security;

-- Policies (Simplified for development)
create policy "allow_all" on public.profiles for all to anon, authenticated using (true) with check (true);
create policy "allow_all" on public.products for all to anon, authenticated using (true) with check (true);
create policy "allow_all" on public.orders for all to anon, authenticated using (true) with check (true);

commit;
