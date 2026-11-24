-- Endurecimento do Supabase (aplicado e verificado em 23/11/2026)
-- Projeto: yfuuhudnvjepyvpewkyg. Para reaplicar: SQL Editor > cole tudo > Run.

-- 1) Tira permissões que não precisam existir
revoke truncate, references, trigger on public.devices from authenticated, anon, public;

-- 2) Fecha funções internas
revoke execute on function public.rls_auto_enable() from public, anon, authenticated;
revoke execute on function public.bump_version() from public, anon, authenticated;

-- 3) register_device com limites (300 novos aparelhos/hora, chave de 32 a 128 caracteres)
create or replace function public.register_device(p_code text, p_secret text)
 returns text
 language plpgsql
 security definer
 set search_path to 'public'
as $function$
declare h text; n int;
begin
  if p_code !~ '^[0-9A-F]{12}$' or length(p_secret) < 32 or length(p_secret) > 128 then
    return 'bad';
  end if;
  h := encode(sha256(convert_to(p_secret, 'utf8')), 'hex');
  if exists (select 1 from devices where code = p_code and secret_hash = h) then
    return 'ok';
  end if;
  if exists (select 1 from devices where code = p_code) then
    return 'taken';
  end if;
  delete from devices
   where mode is null and coalesce(last_seen, created_at) < now() - interval '30 days';
  select count(*) into n from devices
   where mode is null and created_at > now() - interval '1 hour';
  if n >= 300 then
    return 'busy';
  end if;
  insert into devices(code, secret_hash) values (p_code, h)
    on conflict (code) do nothing;
  if found then
    return 'created';
  end if;
  return 'taken';
end $function$;

-- 4) Só o app (anon) e o admin (authenticated) chamam as duas funções
revoke execute on function public.register_device(text, text) from public;
revoke execute on function public.fetch_playlist(text, text) from public;
grant execute on function public.register_device(text, text) to anon, authenticated;
grant execute on function public.fetch_playlist(text, text) to anon, authenticated;
