#!/usr/bin/env node
const fs = require('fs');

let inputData = '';
try { inputData = fs.readFileSync(0, 'utf8'); } catch { console.log(JSON.stringify({ continue: true })); process.exit(0); }

let input;
try { input = JSON.parse(inputData); } catch { console.log(JSON.stringify({ continue: true })); process.exit(0); }

const toolName = input.tool_name;
const toolInput = input.tool_input || {};

if (toolName === 'Bash') {
  const command = toolInput.command || '';

  // 检测 > nul 错误用法
  if (/[12]?\s*>\s*nul\b/i.test(command)) {
    console.log(JSON.stringify({ decision: 'block', reason: '命令被阻止：检测到 `> nul`，Windows 下会创建名为 nul 的文件。请使用 `> /dev/null 2>&1`' }));
    process.exit(0);
  }

  // 危险命令拦截
  const dangerous = [
    { pattern: /rm\s+-rf\s+\/(?!\w)/, reason: '删除根目录' },
    { pattern: /rm\s+-rf\s+\*/, reason: '删除所有文件' },
    { pattern: /drop\s+database/i, reason: '删除数据库' },
    { pattern: /truncate\s+table/i, reason: '清空表数据' },
    { pattern: /git\s+push\s+--force\s+(origin\s+)?(main|master)/i, reason: '强制推送到主分支' },
    { pattern: /git\s+reset\s+--hard\s+HEAD~\d+/, reason: '硬重置多个提交' },
    { pattern: /mkfs\./, reason: '格式化文件系统' },
  ];

  for (const { pattern, reason } of dangerous) {
    if (pattern.test(command)) {
      console.log(JSON.stringify({ decision: 'block', reason: `危险操作被阻止：${reason}\n\n命令: ${command}\n\n如确需执行，请手动在终端运行` }));
      process.exit(0);
    }
  }
}

console.log(JSON.stringify({ continue: true }));
