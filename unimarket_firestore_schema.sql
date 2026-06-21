-- UniMarket Firestore schema simulation
-- Generated on 2026-05-31.
-- This file models active Firestore collections as relational tables.
-- product_images is intentionally absent because the app now reads product.image_urls.
-- discount_codes is currently legacy: the app model exists, but active checkout logic uses fixed codes.

CREATE TABLE profiles (
  id TEXT PRIMARY KEY,
  full_name TEXT NOT NULL,
  phone TEXT,
  university TEXT,
  avatar_url TEXT,
  _verified BOOLEAN NOT NULL DEFAULT FALSE,
  role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'moderator', 'admin')),
  account_status TEXT NOT NULL DEFAULT 'active' CHECK (account_status IN ('active', 'disabled')),
  created_at TEXT,
  updated_at TEXT
);

CREATE TABLE categories (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  parent_id TEXT,
  "order" INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE products (
  id TEXT PRIMARY KEY,
  seller_id TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT,
  price INTEGER NOT NULL DEFAULT 0,
  category_id TEXT NOT NULL,
  condition TEXT CHECK (condition IN ('new', 'used', 'good', 'NEW', 'USED')),
  status TEXT NOT NULL DEFAULT 'active',
  image_urls TEXT,
  created_at TEXT,
  updated_at TEXT,
  FOREIGN KEY (seller_id) REFERENCES profiles(id),
  FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE wishlist (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  created_at TEXT,
  UNIQUE (user_id, product_id),
  FOREIGN KEY (user_id) REFERENCES profiles(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE carts (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  created_at TEXT,
  FOREIGN KEY (user_id) REFERENCES profiles(id)
);

CREATE TABLE cart_items (
  id TEXT PRIMARY KEY,
  cart_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
  FOREIGN KEY (cart_id) REFERENCES carts(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE orders (
  id TEXT PRIMARY KEY,
  buyer_id TEXT NOT NULL,
  seller_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  product_title TEXT NOT NULL,
  product_image_url TEXT,
  quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
  unit_price INTEGER NOT NULL DEFAULT 0,
  discount_code TEXT,
  discount_amount INTEGER NOT NULL DEFAULT 0,
  total_price INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'pending'
    CHECK (status IN ('pending', 'confirmed', 'shipping', 'done', 'cancelled')),
  created_at TEXT,
  updated_at TEXT,
  FOREIGN KEY (buyer_id) REFERENCES profiles(id),
  FOREIGN KEY (seller_id) REFERENCES profiles(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE order_items (
  id TEXT PRIMARY KEY,
  order_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  seller_id TEXT NOT NULL,
  price INTEGER NOT NULL DEFAULT 0,
  quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
  FOREIGN KEY (order_id) REFERENCES orders(id),
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (seller_id) REFERENCES profiles(id)
);

CREATE TABLE conversations (
  id TEXT PRIMARY KEY,
  created_at TEXT,
  updated_at TEXT,
  product_id TEXT NOT NULL,
  product_title TEXT,
  product_image_url TEXT,
  buyer_id TEXT NOT NULL,
  seller_id TEXT NOT NULL,
  buyer_name TEXT,
  seller_name TEXT,
  last_message TEXT,
  last_sender_id TEXT,
  last_message_at TEXT,
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (buyer_id) REFERENCES profiles(id),
  FOREIGN KEY (seller_id) REFERENCES profiles(id),
  FOREIGN KEY (last_sender_id) REFERENCES profiles(id)
);

CREATE TABLE messages (
  id TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL,
  sender_id TEXT NOT NULL,
  content TEXT NOT NULL,
  created_at TEXT,
  FOREIGN KEY (conversation_id) REFERENCES conversations(id),
  FOREIGN KEY (sender_id) REFERENCES profiles(id)
);

CREATE TABLE notifications (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  title TEXT NOT NULL,
  content TEXT,
  type TEXT,
  target_id TEXT,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TEXT,
  FOREIGN KEY (user_id) REFERENCES profiles(id)
);

CREATE TABLE reviews (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL,
  seller_id TEXT NOT NULL,
  reviewer_id TEXT NOT NULL,
  reviewer_name TEXT,
  reviewer_avatar TEXT,
  rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
  title TEXT,
  content TEXT,
  created_at_timestamp TEXT,
  helpful_count INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (seller_id) REFERENCES profiles(id),
  FOREIGN KEY (reviewer_id) REFERENCES profiles(id)
);

CREATE TABLE reports (
  id TEXT PRIMARY KEY,
  reporter_id TEXT NOT NULL,
  product_id TEXT NOT NULL,
  reason TEXT NOT NULL,
  created_at TEXT,
  FOREIGN KEY (reporter_id) REFERENCES profiles(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE student_verifications (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  method TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
  proof_url TEXT,
  student_id TEXT,
  note TEXT,
  created_at TEXT,
  reviewed_at TEXT,
  FOREIGN KEY (user_id) REFERENCES profiles(id)
);

CREATE TABLE user_behavior (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  product_id TEXT,
  action TEXT NOT NULL CHECK (action IN ('view', 'search', 'wishlist', 'cart', 'purchase')),
  created_at TEXT,
  FOREIGN KEY (user_id) REFERENCES profiles(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE discount_codes (
  id TEXT PRIMARY KEY,
  code TEXT NOT NULL UNIQUE,
  description TEXT,
  discount_amount INTEGER NOT NULL DEFAULT 0,
  min_order_amount INTEGER NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  starts_at TEXT,
  ends_at TEXT
);
