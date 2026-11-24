-- =====================================================================
-- FocusLock Supabase Database Migration
-- Implements:
--   1. 8 Professional Focus Modes & Sessions Architecture
--   2. Real-Time Focus Room Group Chat
--   3. First-Class Bedtime Lock Schedules & Statistics
-- Includes Row Level Security (RLS) policies and indexes.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. FOCUS MODES TABLE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.focus_modes (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    default_duration_minutes INT NOT NULL DEFAULT 25,
    default_break_minutes INT NOT NULL DEFAULT 5,
    intensity TEXT NOT NULL DEFAULT 'Medium Intensity',
    icon_name TEXT NOT NULL DEFAULT 'Timer',
    is_system BOOLEAN NOT NULL DEFAULT true,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.focus_modes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read system focus modes and their own custom modes"
    ON public.focus_modes FOR SELECT
    USING (is_system = true OR auth.uid() = user_id);

CREATE POLICY "Users can create custom focus modes"
    ON public.focus_modes FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their custom focus modes"
    ON public.focus_modes FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their custom focus modes"
    ON public.focus_modes FOR DELETE
    USING (auth.uid() = user_id);

-- Seed standard 8 Focus Modes
INSERT INTO public.focus_modes (id, name, description, default_duration_minutes, default_break_minutes, intensity, icon_name, is_system)
VALUES
    ('deep_work', 'Deep Work', 'Uninterrupted deep concentration with minimal distraction.', 90, 15, 'High Intensity', 'Psychology', true),
    ('study', 'Study', 'Balanced 50-minute study sessions with 10-minute mental resets.', 50, 10, 'Medium Intensity', 'School', true),
    ('pomodoro', 'Pomodoro', '25-minute sprints with 5-minute short breaks and 20-minute long breaks.', 25, 5, 'Focus Cycles', 'Timer', true),
    ('reading', 'Reading', 'Quiet reading immersion protecting designated reader application.', 30, 5, 'Quiet Focus', 'Book', true),
    ('quick_focus', 'Quick Focus', 'Rapid 15-minute bursts to complete bite-sized tasks.', 15, 3, 'Light Focus', 'Bolt', true),
    ('strict_focus', 'Strict Focus', 'High-accountability lock with strict cancellation confirmation.', 60, 10, 'Strict Shield', 'Lock', true),
    ('relax_recharge', 'Relax & Recharge', '10-minute guided breathing and parasympathetic recovery.', 10, 0, 'Recovery & Breath', 'SelfImprovement', true),
    ('custom', 'Custom Focus', 'Personalized focus and break intervals tailored to your workflow.', 25, 5, 'Personalized', 'Tune', true)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    default_duration_minutes = EXCLUDED.default_duration_minutes,
    default_break_minutes = EXCLUDED.default_break_minutes,
    intensity = EXCLUDED.intensity,
    icon_name = EXCLUDED.icon_name;

-- ---------------------------------------------------------------------
-- 2. FOCUS SESSIONS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.focus_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    mode_id TEXT REFERENCES public.focus_modes(id),
    target_duration_seconds BIGINT NOT NULL,
    elapsed_duration_seconds BIGINT NOT NULL,
    blocked_attempts_count INT NOT NULL DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    reading_app_package TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.focus_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own focus sessions"
    ON public.focus_sessions FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_focus_sessions_user_time
    ON public.focus_sessions(user_id, started_at DESC);

-- ---------------------------------------------------------------------
-- 3. FOCUS SETTINGS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.focus_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    is_whitelist_mode BOOLEAN NOT NULL DEFAULT false,
    auto_dnd_enabled BOOLEAN NOT NULL DEFAULT true,
    ambient_sound_id TEXT NOT NULL DEFAULT 'none',
    partner_name TEXT DEFAULT '',
    partner_email TEXT DEFAULT '',
    notify_partner_on_quit BOOLEAN NOT NULL DEFAULT false,
    is_parental_lock_enabled BOOLEAN NOT NULL DEFAULT false,
    parental_pin_hash TEXT DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.focus_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own focus settings"
    ON public.focus_settings FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ---------------------------------------------------------------------
-- 4. DAILY FOCUS STATISTICS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.daily_focus_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    total_focus_minutes INT NOT NULL DEFAULT 0,
    completed_sessions_count INT NOT NULL DEFAULT 0,
    interrupted_sessions_count INT NOT NULL DEFAULT 0,
    distraction_blocks_count INT NOT NULL DEFAULT 0,
    most_used_mode TEXT,
    current_streak_days INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, date)
);

ALTER TABLE public.daily_focus_statistics ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view and update their own daily stats"
    ON public.daily_focus_statistics FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_daily_focus_stats_user_date
    ON public.daily_focus_statistics(user_id, date DESC);

-- ---------------------------------------------------------------------
-- 5. REAL-TIME FOCUS ROOM MESSAGES TABLE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.focus_room_messages (
    id TEXT PRIMARY KEY,
    room_id TEXT NOT NULL,
    sender_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    sender_name TEXT NOT NULL,
    content TEXT NOT NULL,
    attachment_type TEXT,
    attachment_name TEXT,
    attachment_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.focus_room_messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read messages in rooms"
    ON public.focus_room_messages FOR SELECT
    USING (true);

CREATE POLICY "Authenticated users can post messages"
    ON public.focus_room_messages FOR INSERT
    WITH CHECK (auth.uid() = sender_id);

CREATE INDEX IF NOT EXISTS idx_room_messages_room_time
    ON public.focus_room_messages(room_id, created_at ASC);

-- ---------------------------------------------------------------------
-- 6. BEDTIME SCHEDULES TABLE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.bedtime_schedules (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    is_enabled BOOLEAN NOT NULL DEFAULT false,
    bedtime_hour INT NOT NULL DEFAULT 22,
    bedtime_minute INT NOT NULL DEFAULT 0,
    wake_up_hour INT NOT NULL DEFAULT 6,
    wake_up_minute INT NOT NULL DEFAULT 0,
    active_days TEXT NOT NULL DEFAULT '2,3,4,5,6', -- Mon-Fri Calendar constants
    advance_reminder_minutes INT NOT NULL DEFAULT 30,
    strict_mode BOOLEAN NOT NULL DEFAULT false,
    emergency_cooldown_minutes INT NOT NULL DEFAULT 5,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.bedtime_schedules ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their bedtime schedule"
    ON public.bedtime_schedules FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ---------------------------------------------------------------------
-- 7. BEDTIME SESSIONS & LOGS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.bedtime_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    scheduled_bedtime TIMESTAMPTZ NOT NULL,
    scheduled_wake_up TIMESTAMPTZ NOT NULL,
    actual_lock_start TIMESTAMPTZ NOT NULL DEFAULT now(),
    actual_lock_end TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'completed', -- completed, early_exit, emergency_overridden
    early_exit_reason TEXT,
    distraction_attempts_during_bedtime INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.bedtime_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their bedtime sessions"
    ON public.bedtime_sessions FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_bedtime_sessions_user_time
    ON public.bedtime_sessions(user_id, actual_lock_start DESC);

-- ---------------------------------------------------------------------
-- 8. BEDTIME STATISTICS TABLE
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.bedtime_statistics (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    current_streak_days INT NOT NULL DEFAULT 0,
    longest_streak_days INT NOT NULL DEFAULT 0,
    weekly_adherence_percent INT NOT NULL DEFAULT 0,
    total_hours_locked NUMERIC(6, 2) NOT NULL DEFAULT 0.0,
    early_exit_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.bedtime_statistics ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view and update their bedtime statistics"
    ON public.bedtime_statistics FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
