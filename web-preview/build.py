#!/usr/bin/env python3
"""Build a static AS Academy web preview from a Course Package.

This generator is intentionally dependency-free so every Academy course repository
can publish a browser preview with GitHub Pages without duplicating web runtime
logic. Course repositories keep only their course/branding content; the shared
web presentation stays in AS-Academy-Core.
"""

from __future__ import annotations

import argparse
import html
import json
import shutil
from pathlib import Path
from typing import Any


def read_json(path: Path, default: Any) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError, OSError):
        return default


def find_course_dir(repo_root: Path, explicit: str | None) -> Path | None:
    candidates: list[Path] = []
    if explicit:
        candidates.append((repo_root / explicit).resolve())
    candidates.append(repo_root / "course")

    assets_root = repo_root / "app" / "src" / "main" / "assets" / "course"
    if assets_root.exists():
        candidates.extend(sorted(p for p in assets_root.iterdir() if p.is_dir()))

    for candidate in candidates:
        if (candidate / "course.json").exists():
            return candidate
    return None


def collect_json_files(folder: Path) -> list[str]:
    if not folder.exists():
        return []
    return [p.relative_to(folder).as_posix() for p in sorted(folder.rglob("*.json"))]


def safe_repo_title(repo_root: Path) -> str:
    name = repo_root.name.replace("-", " ").strip()
    return name or "AS Academy"


def build_config(repo_root: Path, course_dir: Path | None) -> dict[str, Any]:
    if course_dir is None:
        return {
            "courseId": repo_root.name.lower(),
            "slug": repo_root.name,
            "title": safe_repo_title(repo_root),
            "programmingLanguage": "",
            "referenceVersion": "",
            "levels": [],
            "hasCoursePackage": False,
        }

    course = read_json(course_dir / "course.json", {})
    if not isinstance(course, dict):
        course = {}
    return {
        "courseId": course.get("courseId") or course.get("id") or repo_root.name.lower(),
        "slug": course.get("slug") or repo_root.name,
        "title": course.get("title") or course.get("titleFa") or safe_repo_title(repo_root),
        "programmingLanguage": course.get("programmingLanguage") or "",
        "referenceVersion": course.get("referenceVersion") or "",
        "levels": course.get("levels") if isinstance(course.get("levels"), list) else [],
        "hasCoursePackage": True,
    }


def write_site(output: Path, config: dict[str, Any], catalog: dict[str, list[str]]) -> None:
    title = html.escape(str(config.get("title") or "AS Academy"))
    output.mkdir(parents=True, exist_ok=True)

    (output / "config.json").write_text(
        json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (output / "catalog.json").write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (output / ".nojekyll").write_text("", encoding="utf-8")

    index_html = f'''<!doctype html>
<html lang="fa" dir="rtl">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
  <meta name="theme-color" content="#111827">
  <title>{title} — AS Academy Web</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <header class="topbar">
    <a class="brand" href="#" aria-label="AS Academy home">
      <span class="brand-mark">AS</span>
      <span><strong id="brandTitle">AS Academy</strong><small>Web Preview</small></span>
    </a>
    <div class="top-actions">
      <input id="search" type="search" autocomplete="off" placeholder="جستجو در درس‌ها…" aria-label="جستجو">
      <button id="themeToggle" class="icon-btn" title="تغییر تم" aria-label="تغییر تم">◐</button>
    </div>
  </header>

  <main class="shell">
    <aside class="sidebar">
      <div class="course-head">
        <p class="eyebrow">AS Academy</p>
        <h1 id="courseTitle">{title}</h1>
        <p id="courseMeta" class="muted"></p>
      </div>
      <div class="progress-wrap">
        <div class="progress-copy"><span>پیشرفت مطالعه</span><b id="progressText">۰٪</b></div>
        <div class="progress"><i id="bar"></i></div>
        <small id="stats" class="muted"></small>
      </div>
      <div id="levelFilters" class="chips"></div>
      <nav id="chapters" class="chapter-nav"></nav>
    </aside>

    <section class="content">
      <div id="welcome" class="hero">
        <div>
          <p class="eyebrow">نسخه آنلاین برنامه</p>
          <h2 id="heroTitle">{title}</h2>
          <p>محتوای دوره، فصل‌ها و درس‌ها را مستقیم در مرورگر بررسی کن؛ بدون نصب APK. وضعیت مطالعه و علاقه‌مندی‌ها فقط روی همین مرورگر ذخیره می‌شود.</p>
        </div>
        <div class="hero-badge"><span id="lessonCount">0</span><small>درس</small></div>
      </div>

      <div class="toolbar">
        <div class="segmented" role="group" aria-label="فیلتر درس‌ها">
          <button data-mode="all" class="active">همه</button>
          <button data-mode="done">مطالعه‌شده</button>
          <button data-mode="fav">علاقه‌مندی</button>
        </div>
        <button id="showAll" class="secondary-btn">همه فصل‌ها</button>
      </div>

      <div id="emptyCourse" class="empty" hidden>
        <div class="empty-icon">⌁</div>
        <h3>Course Package هنوز در این ریپو قرار نگرفته است</h3>
        <p>زیرساخت نسخه وب فعال است. به‌محض اضافه‌شدن فایل‌های استاندارد course، همین صفحه خودکار به نسخه کامل دوره تبدیل می‌شود.</p>
      </div>

      <div id="lessons" class="lesson-list"></div>
      <article id="lesson" class="lesson-view" hidden></article>
    </section>
  </main>

  <script src="app.js"></script>
</body>
</html>
'''

    styles_css = r'''*{box-sizing:border-box}:root{--bg:#f4f6fa;--panel:#fff;--text:#172033;--muted:#6b7280;--line:#e4e8f0;--brand:#04aa6d;--brand2:#0d7c56;--top:#111827;--soft:#edf9f4;--code:#101826;--shadow:0 14px 40px rgba(17,24,39,.08)}html[data-theme="dark"]{--bg:#0c111b;--panel:#121a27;--text:#eef2f7;--muted:#9ba7b8;--line:#253044;--top:#080d14;--soft:#123126;--code:#060a10;--shadow:0 18px 50px rgba(0,0,0,.28)}body{margin:0;background:var(--bg);color:var(--text);font-family:Tahoma,"Segoe UI",Arial,sans-serif;line-height:1.8}.topbar{height:72px;background:var(--top);color:#fff;display:flex;align-items:center;justify-content:space-between;padding:0 clamp(16px,4vw,54px);position:sticky;top:0;z-index:20;box-shadow:0 1px 0 rgba(255,255,255,.06)}.brand{display:flex;align-items:center;gap:12px;color:#fff;text-decoration:none}.brand-mark{display:grid;place-items:center;width:40px;height:40px;border-radius:13px;background:linear-gradient(135deg,#20c97b,#078354);font-weight:800;box-shadow:0 8px 24px rgba(4,170,109,.28)}.brand span:last-child{display:flex;flex-direction:column;line-height:1.25}.brand strong{font-size:17px}.brand small{font-size:11px;color:#9ba7b8}.top-actions{display:flex;align-items:center;gap:10px}.top-actions input{width:min(430px,42vw);border:1px solid #283243;background:#1a2432;color:#fff;padding:11px 14px;border-radius:12px;outline:none}.top-actions input:focus{border-color:#28c887;box-shadow:0 0 0 3px rgba(40,200,135,.14)}.icon-btn,.secondary-btn,.segmented button,.chapter-btn,.lesson-card,.action-btn,.back-btn{font:inherit}.icon-btn{border:1px solid #2a3547;background:#182231;color:#fff;width:42px;height:42px;border-radius:12px;cursor:pointer}.shell{display:grid;grid-template-columns:310px minmax(0,1fr);min-height:calc(100vh - 72px)}.sidebar{background:var(--panel);border-left:1px solid var(--line);padding:28px 22px;position:sticky;top:72px;height:calc(100vh - 72px);overflow:auto}.eyebrow{margin:0 0 6px;color:var(--brand);font-size:12px;font-weight:800;letter-spacing:.04em;text-transform:uppercase}.course-head h1{font-size:22px;line-height:1.45;margin:0 0 6px}.muted{color:var(--muted)}.progress-wrap{padding:18px 0;border-bottom:1px solid var(--line)}.progress-copy{display:flex;justify-content:space-between;gap:12px;font-size:13px}.progress{height:9px;border-radius:999px;background:var(--line);overflow:hidden;margin:10px 0 7px}.progress i{display:block;width:0;height:100%;background:linear-gradient(90deg,var(--brand2),#2bd88c);transition:width .25s ease}.chips{display:flex;flex-wrap:wrap;gap:7px;padding:16px 0}.chips button{border:1px solid var(--line);color:var(--muted);background:var(--panel);padding:6px 10px;border-radius:999px;cursor:pointer}.chips button.active{background:var(--soft);color:var(--brand2);border-color:rgba(4,170,109,.25)}.chapter-nav{display:flex;flex-direction:column;gap:5px}.chapter-btn{width:100%;border:0;background:transparent;color:var(--text);text-align:right;padding:10px 12px;border-radius:10px;cursor:pointer}.chapter-btn:hover,.chapter-btn.active{background:var(--soft);color:var(--brand2)}.content{width:min(1080px,100%);padding:clamp(20px,4vw,48px);margin:0 auto}.hero{display:flex;align-items:center;justify-content:space-between;gap:30px;background:linear-gradient(135deg,var(--panel),var(--soft));border:1px solid var(--line);border-radius:24px;padding:clamp(22px,4vw,40px);box-shadow:var(--shadow)}.hero h2{font-size:clamp(26px,4vw,42px);line-height:1.35;margin:4px 0 14px}.hero p{max-width:720px;margin:0}.hero-badge{flex:0 0 112px;height:112px;border-radius:28px;display:grid;place-items:center;align-content:center;background:var(--top);color:#fff;box-shadow:0 18px 40px rgba(17,24,39,.18)}.hero-badge span{font-size:34px;font-weight:800;line-height:1}.hero-badge small{margin-top:8px;color:#b7c0cf}.toolbar{display:flex;justify-content:space-between;align-items:center;gap:12px;margin:24px 0 14px}.segmented{display:flex;gap:6px;background:var(--panel);border:1px solid var(--line);padding:5px;border-radius:13px}.segmented button,.secondary-btn{border:0;background:transparent;color:var(--muted);padding:8px 12px;border-radius:9px;cursor:pointer}.segmented button.active{background:var(--soft);color:var(--brand2);font-weight:700}.secondary-btn{background:var(--panel);border:1px solid var(--line)}.lesson-list{display:grid;gap:10px}.lesson-card{display:grid;grid-template-columns:1fr auto;gap:18px;align-items:center;width:100%;text-align:right;border:1px solid var(--line);background:var(--panel);color:var(--text);padding:17px 18px;border-radius:15px;cursor:pointer;transition:transform .12s ease,border-color .12s ease,box-shadow .12s ease}.lesson-card:hover{transform:translateY(-1px);border-color:rgba(4,170,109,.35);box-shadow:0 10px 26px rgba(17,24,39,.06)}.lesson-card strong{display:block;font-size:15px}.lesson-card .lesson-sub{display:flex;gap:8px;flex-wrap:wrap;color:var(--muted);font-size:12px}.status-icons{display:flex;gap:7px;font-size:14px;color:var(--brand2)}.lesson-view{background:var(--panel);border:1px solid var(--line);border-radius:22px;padding:clamp(20px,4vw,42px);box-shadow:var(--shadow)}.lesson-view h1{font-size:clamp(25px,4vw,38px);line-height:1.45;margin:14px 0}.lesson-view h2,.lesson-view h3{line-height:1.5;margin-top:28px}.lesson-view pre{direction:ltr;text-align:left;background:var(--code);color:#e7edf6;padding:18px;border-radius:14px;overflow:auto;line-height:1.6}.lesson-view code{font-family:Consolas,"Courier New",monospace}.callout{padding:15px 17px;border-radius:13px;background:var(--soft);border-right:4px solid var(--brand);margin:15px 0}.callout.warning{background:#fff7df;border-color:#e8a40c;color:#604500}.callout.exercise{background:#f2ecff;border-color:#7c55d7;color:#3c276d}.actions{display:flex;gap:9px;flex-wrap:wrap;margin:18px 0}.action-btn,.back-btn{border:1px solid var(--line);background:var(--panel);color:var(--text);padding:9px 13px;border-radius:10px;cursor:pointer}.action-btn.primary,.action-btn.active{background:var(--brand);border-color:var(--brand);color:#fff}.empty{padding:44px 24px;text-align:center;background:var(--panel);border:1px dashed var(--line);border-radius:20px}.empty-icon{font-size:40px;color:var(--brand)}@media(max-width:860px){.shell{display:block}.sidebar{position:static;height:auto;border-left:0;border-bottom:1px solid var(--line)}.chapter-nav{display:grid;grid-template-columns:repeat(2,minmax(0,1fr))}.content{padding:18px}.hero{align-items:flex-start}.hero-badge{display:none}}@media(max-width:580px){.topbar{height:auto;min-height:72px;gap:10px;padding:12px 14px}.brand strong{font-size:14px}.top-actions input{width:46vw}.shell{min-height:0}.chapter-nav{grid-template-columns:1fr}.toolbar{align-items:stretch;flex-direction:column}.segmented{overflow:auto}.segmented button{white-space:nowrap}.lesson-card{grid-template-columns:1fr}.content{padding:14px}.hero,.lesson-view{border-radius:17px}}
'''

    app_js = r'''const $=s=>document.querySelector(s);const $$=s=>[...document.querySelectorAll(s)];let config={},catalog={},chapters=[],lessons=[],mode='all',chapterFilter=null,levelFilter=null;let done=new Set(),fav=new Set();const stateKey=s=>`as-academy.${config.courseId||'course'}.${s}`;const loadState=()=>{done=new Set(JSON.parse(localStorage.getItem(stateKey('done'))||'[]'));fav=new Set(JSON.parse(localStorage.getItem(stateKey('fav'))||'[]'))};const saveState=()=>{localStorage.setItem(stateKey('done'),JSON.stringify([...done]));localStorage.setItem(stateKey('fav'),JSON.stringify([...fav]));updateStats()};const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));const faNum=n=>Number(n||0).toLocaleString('fa-IR');function blockText(b){return b.textFa??b.text??b.contentFa??b.content??b.titleFa??b.title??''}async function fetchJson(path,fallback){try{const r=await fetch(path);if(!r.ok)throw new Error(r.status);return await r.json()}catch{return fallback}}async function load(){config=await fetchJson('config.json',{});catalog=await fetchJson('catalog.json',{});document.title=`${config.title||'AS Academy'} — AS Academy Web`;$('#brandTitle').textContent=config.programmingLanguage?`AS Academy ${config.programmingLanguage}`:'AS Academy';$('#courseTitle').textContent=config.title||'AS Academy';$('#heroTitle').textContent=config.title||'AS Academy';$('#courseMeta').textContent=[config.programmingLanguage,config.referenceVersion&&`نسخه مرجع ${config.referenceVersion}`].filter(Boolean).join(' • ');if(!config.hasCoursePackage){$('#emptyCourse').hidden=false;$('#welcome').hidden=true;$('.toolbar').hidden=true;return}chapters=await fetchJson('course/chapters.json',[]);chapters=Array.isArray(chapters)?chapters:(chapters.chapters||[]);for(const rel of (catalog.lessons||[])){const item=await fetchJson('course/lessons/'+rel,null);if(item)lessons.push(item)}lessons.sort((a,b)=>(a.order||0)-(b.order||0));loadState();$('#lessonCount').textContent=faNum(lessons.length);renderLevels();renderChapters();renderList();}function updateStats(){const pct=lessons.length?Math.round(done.size/lessons.length*100):0;$('#stats').textContent=`${faNum(done.size)} از ${faNum(lessons.length)} درس مطالعه شده`;$('#progressText').textContent=faNum(pct)+'٪';$('#bar').style.width=pct+'%'}function renderLevels(){const host=$('#levelFilters');const levels=Array.isArray(config.levels)?config.levels:[];if(!levels.length){host.hidden=true;return}host.innerHTML='<button data-level="" class="active">همه سطح‌ها</button>'+levels.map(x=>`<button data-level="${esc(x.id)}">${esc(x.title||x.titleFa||x.id)}</button>`).join('');host.addEventListener('click',e=>{const b=e.target.closest('button');if(!b)return;levelFilter=b.dataset.level||null;chapterFilter=null;$$('#levelFilters button').forEach(x=>x.classList.toggle('active',x===b));renderChapters();renderList()})}function visibleChapters(){if(!levelFilter)return chapters;return chapters.filter(c=>c.levelId===levelFilter)}function renderChapters(){const host=$('#chapters');const items=visibleChapters();host.innerHTML=items.map(c=>`<button class="chapter-btn" data-chapter="${esc(c.id)}">${esc(c.titleFa||c.title||c.id)}</button>`).join('');host.onclick=e=>{const b=e.target.closest('[data-chapter]');if(!b)return;chapterFilter=b.dataset.chapter;$$('.chapter-btn').forEach(x=>x.classList.toggle('active',x===b));renderList()}}function setMode(next){mode=next;$$('.segmented button').forEach(b=>b.classList.toggle('active',b.dataset.mode===mode));renderList()}function renderList(){$('#lesson').hidden=true;$('#welcome').hidden=false;const q=$('#search').value.trim().toLowerCase();let list=lessons.filter(l=>{const ch=chapters.find(c=>c.id===l.chapterId);const levelOk=!levelFilter||(ch&&ch.levelId===levelFilter);const chapterOk=!chapterFilter||l.chapterId===chapterFilter;const searchOk=!q||JSON.stringify(l).toLowerCase().includes(q);const modeOk=mode==='all'||(mode==='done'&&done.has(l.id))||(mode==='fav'&&fav.has(l.id));return levelOk&&chapterOk&&searchOk&&modeOk});$('#lessons').innerHTML=list.map(l=>{const ch=chapters.find(c=>c.id===l.chapterId);const mins=l.durationMin||l.estimatedMinutes;return `<button class="lesson-card" data-id="${esc(l.id)}"><span><strong>${esc(l.titleFa||l.title||l.id)}</strong><span class="lesson-sub"><span>${esc(ch?.titleFa||ch?.title||'')}</span>${mins?`<span>${faNum(mins)} دقیقه</span>`:''}</span></span><span class="status-icons">${done.has(l.id)?'<span title="مطالعه‌شده">✓</span>':''}${fav.has(l.id)?'<span title="علاقه‌مندی">★</span>':''}</span></button>`}).join('')||'<div class="empty"><h3>درسی پیدا نشد</h3><p>فیلتر یا عبارت جستجو را تغییر بده.</p></div>';$('#lessons').onclick=e=>{const b=e.target.closest('[data-id]');if(b)openLesson(b.dataset.id)};updateStats()}function renderBlock(b){const type=String(b.type||'').toUpperCase(),text=blockText(b);if(type==='CODE')return `<pre><code>${esc(b.code??text)}</code></pre>`;if(type==='OUTPUT')return `<pre><code>${esc(text)}</code></pre>`;if(type==='TITLE')return `<h2>${esc(text)}</h2>`;if(type==='SUBTITLE')return `<h3>${esc(text)}</h3>`;if(type==='LIST'&&Array.isArray(b.items))return '<ul>'+b.items.map(x=>`<li>${esc(typeof x==='string'?x:(x.textFa??x.text??x.title??''))}</li>`).join('')+'</ul>';if(['TIP','NOTE','IMPORTANT'].includes(type))return `<div class="callout"><b>${esc(type)}</b><div>${esc(text)}</div></div>`;if(type==='WARNING')return `<div class="callout warning"><b>هشدار</b><div>${esc(text)}</div></div>`;if(type==='EXERCISE')return `<div class="callout exercise"><b>تمرین</b><div>${esc(text)}</div></div>`;if(type==='QUOTE')return `<blockquote>${esc(text)}</blockquote>`;return text?`<p>${esc(text)}</p>`:''}function openLesson(id){const l=lessons.find(x=>x.id===id);if(!l)return;$('#welcome').hidden=true;$('#lessons').innerHTML='';const article=$('#lesson');article.hidden=false;article.innerHTML=`<button class="back-btn" id="backList">← فهرست درس‌ها</button><h1>${esc(l.titleFa||l.title||id)}</h1>${Array.isArray(l.objectives)&&l.objectives.length?`<div class="callout"><b>هدف‌های یادگیری</b><ul>${l.objectives.map(x=>`<li>${esc(x)}</li>`).join('')}</ul></div>`:''}<div class="actions"><button class="action-btn ${done.has(id)?'active':''}" id="toggleDone">✓ مطالعه‌شده</button><button class="action-btn ${fav.has(id)?'active':''}" id="toggleFav">★ علاقه‌مندی</button></div>${(l.blocks||[]).map(renderBlock).join('')}<div class="actions"><button class="action-btn" id="prevLesson">درس قبلی</button><button class="action-btn primary" id="nextLesson">درس بعدی</button></div>`;$('#backList').onclick=renderList;$('#toggleDone').onclick=()=>{done.has(id)?done.delete(id):done.add(id);saveState();openLesson(id)};$('#toggleFav').onclick=()=>{fav.has(id)?fav.delete(id):fav.add(id);saveState();openLesson(id)};const i=lessons.findIndex(x=>x.id===id);$('#prevLesson').onclick=()=>lessons[i-1]&&openLesson(lessons[i-1].id);$('#nextLesson').onclick=()=>lessons[i+1]&&openLesson(lessons[i+1].id);window.scrollTo({top:0,behavior:'smooth'})}$$('.segmented button').forEach(b=>b.onclick=()=>setMode(b.dataset.mode));$('#showAll').onclick=()=>{chapterFilter=null;levelFilter=null;$$('#levelFilters button').forEach((b,i)=>b.classList.toggle('active',i===0));renderChapters();renderList()};$('#search').addEventListener('input',renderList);const storedTheme=localStorage.getItem('as-academy.theme');if(storedTheme)document.documentElement.dataset.theme=storedTheme;$('#themeToggle').onclick=()=>{const next=document.documentElement.dataset.theme==='dark'?'light':'dark';document.documentElement.dataset.theme=next;localStorage.setItem('as-academy.theme',next)};load().catch(err=>{$('#lessons').innerHTML=`<div class="empty"><h3>خطا در بارگذاری نسخه وب</h3><p>${esc(err.message)}</p></div>`});
'''

    (output / "index.html").write_text(index_html, encoding="utf-8")
    (output / "styles.css").write_text(styles_css, encoding="utf-8")
    (output / "app.js").write_text(app_js, encoding="utf-8")


def build(repo_root: Path, course_dir_arg: str | None, output: Path) -> None:
    course_dir = find_course_dir(repo_root, course_dir_arg)
    config = build_config(repo_root, course_dir)

    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True, exist_ok=True)

    catalog: dict[str, list[str]] = {
        "lessons": [],
        "exercises": [],
        "quizzes": [],
        "projects": [],
    }

    if course_dir is not None:
        target_course = output / "course"
        shutil.copytree(course_dir, target_course)
        for kind in catalog:
            catalog[kind] = collect_json_files(course_dir / kind)

    write_site(output, config, catalog)
    print(
        f"AS Academy web preview built: {output} | "
        f"course={config.get('courseId')} | lessons={len(catalog['lessons'])}"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Build AS Academy static web preview")
    parser.add_argument("--repo-root", default=".", help="Course repository root")
    parser.add_argument("--course-dir", default=None, help="Optional course directory relative to repo root")
    parser.add_argument("--output", default="web-dist", help="Output directory")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    output = Path(args.output)
    if not output.is_absolute():
        output = (repo_root / output).resolve()
    build(repo_root, args.course_dir, output)


if __name__ == "__main__":
    main()
