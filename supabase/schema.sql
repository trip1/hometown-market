create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null check (char_length(display_name) between 2 and 80),
  neighborhood text,
  created_at timestamptz not null default now()
);
create table if not exists public.listings (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null check (char_length(title) between 3 and 120),
  description text not null check (char_length(description) between 10 and 4000),
  offer_type text not null check (offer_type in ('cash','cash_only','trade','trade_only')),
  price_cents integer check (price_cents >= 0),
  neighborhood text not null check (char_length(neighborhood) between 2 and 120),
  status text not null default 'active' check (status in ('active','sold','archived')),
  created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table if not exists public.listing_images (
  id uuid primary key default gen_random_uuid(),
  listing_id uuid not null references public.listings(id) on delete cascade,
  owner_id uuid not null references auth.users(id) on delete cascade,
  object_path text not null unique,
  sort_order smallint not null default 0 check (sort_order between 0 and 7),
  created_at timestamptz not null default now()
);
create table if not exists public.comments (
  id uuid primary key default gen_random_uuid(),
  listing_id uuid not null references public.listings(id) on delete cascade,
  author_id uuid not null references auth.users(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 1000),
  created_at timestamptz not null default now()
);
alter table public.profiles enable row level security;
alter table public.listings enable row level security;
alter table public.listing_images enable row level security;
alter table public.comments enable row level security;
create policy "active listings are public" on public.listings for select using (status = 'active' or owner_id = auth.uid());
create policy "owners create listings" on public.listings for insert to authenticated with check (owner_id = auth.uid());
create policy "owners update listings" on public.listings for update to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "owners delete listings" on public.listings for delete to authenticated using (owner_id = auth.uid());
create policy "listing images are public" on public.listing_images for select using (exists (select 1 from public.listings l where l.id = listing_id and l.status = 'active'));
create policy "owners manage image rows" on public.listing_images for all to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "comments on active listings are public" on public.comments for select using (exists (select 1 from public.listings l where l.id = listing_id and l.status = 'active'));
create policy "authenticated users comment" on public.comments for insert to authenticated with check (author_id = auth.uid());
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types) values ('listing-images','listing-images',true,10485760,array['image/jpeg','image/png','image/webp']) on conflict (id) do nothing;
create policy "listing images public read" on storage.objects for select using (bucket_id = 'listing-images');
create policy "users upload own listing images" on storage.objects for insert to authenticated with check (bucket_id = 'listing-images' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "users delete own listing images" on storage.objects for delete to authenticated using (bucket_id = 'listing-images' and owner_id = auth.uid()::text);
