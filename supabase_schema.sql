-- ==========================================================
-- Miogram Supabase Database Schema
-- Table: miogram_badges
-- Purpose: Global badge resolution & community presence sync
-- ==========================================================

create table if not exists public.miogram_badges (
    user_id bigint primary key,
    badge_id text not null default 'original',
    is_active boolean not null default true,
    client_version text default 'Miogram 1.0',
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

alter table public.miogram_badges enable row level security;

drop policy if exists "Allow public read of active badges" on public.miogram_badges;
create policy "Allow public read of active badges"
    on public.miogram_badges
    for select
    using (true);

drop policy if exists "Allow public insert or upsert" on public.miogram_badges;
create policy "Allow public insert or upsert"
    on public.miogram_badges
    for insert
    with check (true);

drop policy if exists "Allow public update" on public.miogram_badges;
create policy "Allow public update"
    on public.miogram_badges
    for update
    using (true);

create index if not exists idx_miogram_badges_lookup on public.miogram_badges (user_id, is_active);

insert into public.miogram_badges (user_id, badge_id, is_active, client_version)
values (8011880648, 'original', true, 'Founder Edition')
on conflict (user_id) do update
set badge_id = excluded.badge_id,
    is_active = excluded.is_active,
    updated_at = timezone('utc'::text, now());
