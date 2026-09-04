-- ==========================================================
-- Miogram Supabase Database Schema
-- Table: miogram_badges
-- Purpose: Global badge resolution, community presence & badge lore
-- ==========================================================

create table if not exists public.miogram_badges (
    user_id bigint primary key,
    badge_id text not null default 'original',
    title text not null default 'Miogram Community ໒꒱',
    obtained_reason text not null default 'Верифікований учасник спільноти Miogram',
    obtained_at timestamp with time zone default timezone('utc'::text, now()) not null,
    is_active boolean not null default true,
    client_version text default 'Miogram 1.0',
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Safely add any new columns if the table already existed
alter table public.miogram_badges add column if not exists title text default 'Miogram Community ໒꒱';
alter table public.miogram_badges add column if not exists obtained_reason text default 'Верифікований учасник спільноти Miogram';
alter table public.miogram_badges add column if not exists obtained_at timestamp with time zone default timezone('utc'::text, now());

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

-- Pre-seed Founder badge (8011880648) with official lore & obtain reason
insert into public.miogram_badges (user_id, badge_id, title, obtained_reason, obtained_at, is_active, client_version)
values (
    8011880648,
    'original',
    'Засновник & Архітектор Miogram ໒꒱',
    'Створено автором Miogram як першу канонічну відзнаку екосистеми з моменту заснування проекту (01.09.2026).',
    '2026-09-01T00:00:00Z',
    true,
    'Founder Edition'
)
on conflict (user_id) do update
set badge_id = excluded.badge_id,
    title = excluded.title,
    obtained_reason = excluded.obtained_reason,
    is_active = excluded.is_active,
    updated_at = timezone('utc'::text, now());
