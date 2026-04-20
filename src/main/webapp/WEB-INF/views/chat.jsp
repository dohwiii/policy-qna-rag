<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>사내 정책 Q&A</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #f0f2f5;
            height: 100vh;
            display: flex;
            flex-direction: column;
        }

        header {
            background: #1e40af;
            color: white;
            padding: 16px 24px;
            display: flex;
            align-items: center;
            gap: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.2);
        }

        header h1 { font-size: 18px; font-weight: 600; }
        header .subtitle { font-size: 12px; opacity: 0.75; margin-top: 2px; }

        header .links {
            margin-left: auto;
            display: flex;
            gap: 12px;
        }

        header .links a {
            color: rgba(255,255,255,0.85);
            text-decoration: none;
            font-size: 13px;
            padding: 4px 10px;
            border: 1px solid rgba(255,255,255,0.3);
            border-radius: 4px;
            transition: background 0.2s;
        }

        header .links a:hover { background: rgba(255,255,255,0.15); }

        #messages {
            flex: 1;
            overflow-y: auto;
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .message { display: flex; gap: 10px; max-width: 80%; }
        .message.user { align-self: flex-end; flex-direction: row-reverse; }

        .avatar {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 16px;
            flex-shrink: 0;
        }

        .message.bot .avatar { background: #dbeafe; }
        .message.user .avatar { background: #1e40af; color: white; }

        .bubble {
            padding: 12px 16px;
            border-radius: 16px;
            line-height: 1.6;
            font-size: 14px;
            word-break: break-word;
        }

        .message.bot .bubble {
            background: white;
            border-top-left-radius: 4px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.08);
            color: #1f2937;
        }

        .message.user .bubble {
            background: #1e40af;
            color: white;
            border-top-right-radius: 4px;
        }

        .cursor {
            display: inline-block;
            width: 2px;
            height: 14px;
            background: #6b7280;
            margin-left: 2px;
            animation: blink 0.8s infinite;
            vertical-align: middle;
        }

        @keyframes blink { 0%,100% { opacity: 1; } 50% { opacity: 0; } }

        .sources {
            margin-top: 12px;
            border-top: 1px solid #e5e7eb;
            padding-top: 10px;
        }

        .sources-toggle {
            background: none;
            border: none;
            color: #6b7280;
            font-size: 12px;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 4px;
            padding: 0;
        }

        .sources-toggle:hover { color: #374151; }

        .sources-list {
            margin-top: 8px;
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .source-item {
            background: #f9fafb;
            border: 1px solid #e5e7eb;
            border-radius: 8px;
            padding: 8px 12px;
            font-size: 12px;
        }

        .source-title { font-weight: 600; color: #374151; }
        .source-meta { color: #6b7280; margin-top: 2px; }
        .source-snippet { color: #4b5563; margin-top: 4px; font-style: italic; }

        .score-badge {
            display: inline-block;
            background: #dbeafe;
            color: #1e40af;
            padding: 1px 6px;
            border-radius: 10px;
            font-size: 11px;
            margin-left: 6px;
        }

        .related-terms {
            margin-top: 10px;
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }

        .term-chip {
            background: #f0fdf4;
            border: 1px solid #bbf7d0;
            color: #166534;
            padding: 3px 10px;
            border-radius: 12px;
            font-size: 12px;
            cursor: help;
        }

        .welcome {
            align-self: center;
            text-align: center;
            color: #6b7280;
            max-width: 400px;
            margin: auto;
        }

        .welcome h2 { font-size: 22px; color: #1f2937; margin-bottom: 8px; }
        .welcome p { font-size: 14px; margin-bottom: 20px; }

        .example-questions {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .example-btn {
            background: white;
            border: 1px solid #d1d5db;
            border-radius: 8px;
            padding: 10px 16px;
            font-size: 13px;
            color: #374151;
            cursor: pointer;
            text-align: left;
            transition: all 0.2s;
        }

        .example-btn:hover {
            border-color: #1e40af;
            color: #1e40af;
            background: #eff6ff;
        }

        .input-area {
            background: white;
            border-top: 1px solid #e5e7eb;
            padding: 16px 24px;
            display: flex;
            gap: 12px;
            align-items: flex-end;
        }

        #question-input {
            flex: 1;
            border: 1px solid #d1d5db;
            border-radius: 12px;
            padding: 12px 16px;
            font-size: 14px;
            resize: none;
            outline: none;
            max-height: 120px;
            line-height: 1.5;
            font-family: inherit;
            transition: border-color 0.2s;
        }

        #question-input:focus { border-color: #1e40af; }

        #send-btn {
            background: #1e40af;
            color: white;
            border: none;
            border-radius: 12px;
            padding: 12px 20px;
            font-size: 14px;
            cursor: pointer;
            font-weight: 500;
            transition: background 0.2s;
            white-space: nowrap;
        }

        #send-btn:hover:not(:disabled) { background: #1d3a9e; }
        #send-btn:disabled { background: #93c5fd; cursor: not-allowed; }
    </style>
</head>
<body>

<header>
    <div>
        <div style="font-size:22px; display:inline; margin-right:8px;">📋</div>
        <span style="display:inline-block; vertical-align:middle;">
            <h1>사내 정책 Q&A</h1>
            <div class="subtitle">문서 기반 AI 답변 시스템</div>
        </span>
    </div>
    <div class="links">
        <a href="/swagger-ui/index.html" target="_blank">API 문서</a>
        <a href="/api/v1/documents" target="_blank">문서 목록</a>
    </div>
</header>

<div id="messages">
    <div class="welcome" id="welcome">
        <h2>안녕하세요!</h2>
        <p>사내 정책, 업무 매뉴얼, 규정에 관한 질문을 입력해 주세요.<br>관련 문서를 검색하여 출처와 함께 답변드립니다.</p>
        <div class="example-questions">
            <button class="example-btn" onclick="sendExample(this)">연차는 몇 일이나 사용할 수 있나요?</button>
            <button class="example-btn" onclick="sendExample(this)">커밋 메시지는 어떻게 작성하나요?</button>
            <button class="example-btn" onclick="sendExample(this)">재택근무 신청 절차가 어떻게 되나요?</button>
        </div>
    </div>
</div>

<div class="input-area">
    <textarea
        id="question-input"
        placeholder="질문을 입력하세요... (Shift+Enter로 줄바꿈, Enter로 전송)"
        rows="1"
        oninput="autoResize(this)"
        onkeydown="handleKey(event)"
    ></textarea>
    <button id="send-btn" onclick="handleSend()">전송</button>
</div>

<script>
    let isStreaming = false;

    function autoResize(el) {
        el.style.height = 'auto';
        el.style.height = Math.min(el.scrollHeight, 120) + 'px';
    }

    function handleKey(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    }

    function sendExample(btn) {
        document.getElementById('question-input').value = btn.textContent;
        handleSend();
    }

    function handleSend() {
        if (isStreaming) return;
        const input = document.getElementById('question-input');
        const question = input.value.trim();
        if (!question) return;

        hideWelcome();
        appendUserMessage(question);
        input.value = '';
        input.style.height = 'auto';

        setStreaming(true);
        streamQuestion(question);
    }

    function hideWelcome() {
        const w = document.getElementById('welcome');
        if (w) w.remove();
    }

    function setStreaming(v) {
        isStreaming = v;
        document.getElementById('send-btn').disabled = v;
        document.getElementById('send-btn').textContent = v ? '생성 중...' : '전송';
    }

    function appendUserMessage(text) {
        const el = document.createElement('div');
        el.className = 'message user';
        el.innerHTML = `
            <div class="avatar">👤</div>
            <div class="bubble">${escapeHtml(text)}</div>
        `;
        messagesEl().appendChild(el);
        scrollToBottom();
    }

    function createBotMessage() {
        const el = document.createElement('div');
        el.className = 'message bot';
        el.innerHTML = `
            <div class="avatar">🤖</div>
            <div class="bubble">
                <span class="answer-text"></span><span class="cursor"></span>
            </div>
        `;
        messagesEl().appendChild(el);
        scrollToBottom();
        return el;
    }

    function appendToken(botEl, text) {
        botEl.querySelector('.answer-text').textContent += text;
        scrollToBottom();
    }

    function finalizeBotMessage(botEl, sources, relatedTerms) {
        botEl.querySelector('.cursor').remove();

        const bubble = botEl.querySelector('.bubble');

        // 출처 섹션
        if (sources && sources.length > 0) {
            const sourcesEl = document.createElement('div');
            sourcesEl.className = 'sources';

            const toggle = document.createElement('button');
            toggle.className = 'sources-toggle';
            toggle.innerHTML = `📄 출처 ${sources.length}개 보기 ▼`;
            toggle.onclick = () => {
                const list = sourcesEl.querySelector('.sources-list');
                const isHidden = list.style.display === 'none';
                list.style.display = isHidden ? 'flex' : 'none';
                toggle.innerHTML = isHidden
                    ? `📄 출처 ${sources.length}개 숨기기 ▲`
                    : `📄 출처 ${sources.length}개 보기 ▼`;
            };

            const list = document.createElement('div');
            list.className = 'sources-list';
            list.style.display = 'none';

            sources.forEach(s => {
                const item = document.createElement('div');
                item.className = 'source-item';
                const score = s.relevanceScore ? (s.relevanceScore * 100).toFixed(0) : '';
                item.innerHTML = `
                    <div class="source-title">
                        ${escapeHtml(s.documentTitle || '문서')}
                        ${s.documentCode ? `<span style="color:#6b7280">(${escapeHtml(s.documentCode)})</span>` : ''}
                        ${score ? `<span class="score-badge">${score}%</span>` : ''}
                    </div>
                    ${s.articleNumber || s.sectionTitle ? `<div class="source-meta">${escapeHtml([s.articleNumber, s.sectionTitle].filter(Boolean).join(' · '))}</div>` : ''}
                    ${s.snippet ? `<div class="source-snippet">"${escapeHtml(s.snippet)}"</div>` : ''}
                `;
                list.appendChild(item);
            });

            sourcesEl.appendChild(toggle);
            sourcesEl.appendChild(list);
            bubble.appendChild(sourcesEl);
        }

        // 관련 용어 칩
        if (relatedTerms && relatedTerms.length > 0) {
            const termsEl = document.createElement('div');
            termsEl.className = 'related-terms';
            relatedTerms.forEach(t => {
                const chip = document.createElement('span');
                chip.className = 'term-chip';
                chip.textContent = t.term;
                chip.title = t.definition || '';
                termsEl.appendChild(chip);
            });
            bubble.appendChild(termsEl);
        }

        scrollToBottom();
    }

    async function streamQuestion(question) {
        const botEl = createBotMessage();

        try {
            const response = await fetch('/api/v1/qna/ask/stream', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ question })
            });

            if (!response.ok) {
                appendToken(botEl, '서버 오류가 발생했습니다. (' + response.status + ')');
                botEl.querySelector('.cursor').remove();
                return;
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = '';
            let lastSources = [];
            let lastRelatedTerms = [];

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop(); // 미완성 라인 보류

                for (const line of lines) {
                    if (!line.startsWith('data:')) continue;
                    const raw = line.slice(5).trim();
                    if (!raw) continue;

                    try {
                        const payload = JSON.parse(raw);
                        if (payload.type === 'token') {
                            appendToken(botEl, payload.text);
                        } else if (payload.type === 'done') {
                            lastSources = payload.sources || [];
                            lastRelatedTerms = payload.relatedTerms || [];
                        } else if (payload.type === 'error') {
                            appendToken(botEl, payload.message || '오류가 발생했습니다.');
                        }
                    } catch (e) {
                        // JSON 파싱 실패 시 무시
                    }
                }
            }

            finalizeBotMessage(botEl, lastSources, lastRelatedTerms);

        } catch (err) {
            appendToken(botEl, '연결 오류: ' + err.message);
            botEl.querySelector('.cursor')?.remove();
        } finally {
            setStreaming(false);
            scrollToBottom();
        }
    }

    function messagesEl() { return document.getElementById('messages'); }

    function scrollToBottom() {
        const el = messagesEl();
        el.scrollTop = el.scrollHeight;
    }

    function escapeHtml(text) {
        if (!text) return '';
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
</script>

</body>
</html>
