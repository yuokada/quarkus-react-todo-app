import { useCallback, useEffect, useMemo, useState } from "react";
import "./App.css";

const API_BASE = "/api/todos";
const AGENT_API = "/api/agent";

const defaultForm = {
  title: "",
  completed: false,
};

function App() {
  const [todos, setTodos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [form, setForm] = useState(defaultForm);
  const [submitting, setSubmitting] = useState(false);
  const [sessionId] = useState(() => crypto.randomUUID());
  const [agentInput, setAgentInput] = useState("");
  const [agentMessages, setAgentMessages] = useState([
    {
      id: crypto.randomUUID(),
      role: "assistant",
      content: "Todoの確認・追加・更新をお手伝いします。削除は実行前に確認します。",
    },
  ]);
  const [agentLoading, setAgentLoading] = useState(false);
  const [agentError, setAgentError] = useState("");

  const pendingCount = useMemo(() => todos.filter((todo) => !todo.completed).length, [todos]);

  const loadTodos = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const response = await fetch(API_BASE);
      if (!response.ok) {
        throw new Error("Failed to fetch todos");
      }
      const data = await response.json();
      setTodos(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Unknown error");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTodos();
  }, [loadTodos]);

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!form.title.trim()) {
      setError("Title is required");
      return;
    }

    try {
      setSubmitting(true);
      setError("");
      const response = await fetch(API_BASE, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: form.title.trim(),
          completed: form.completed,
        }),
      });
      if (!response.ok) {
        throw new Error("Failed to create todo");
      }
      const created = await response.json();
      setTodos((prev) => [...prev, created]);
      setForm(defaultForm);
    } catch (err) {
      setError(err.message || "Unknown error");
    } finally {
      setSubmitting(false);
    }
  };

  const toggleTodo = async (todo) => {
    try {
      setError("");
      const response = await fetch(`${API_BASE}/${todo.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: todo.title,
          completed: !todo.completed,
        }),
      });
      if (!response.ok) {
        throw new Error("Failed to update todo");
      }
      const updated = await response.json();
      setTodos((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
    } catch (err) {
      setError(err.message || "Unknown error");
    }
  };

  const deleteTodo = async (todoId) => {
    try {
      setError("");
      const response = await fetch(`${API_BASE}/${todoId}`, { method: "DELETE" });
      if (!response.ok && response.status !== 404) {
        throw new Error("Failed to delete todo");
      }
      setTodos((prev) => prev.filter((t) => t.id !== todoId));
    } catch (err) {
      setError(err.message || "Unknown error");
    }
  };

  const handleAgentSubmit = async (event) => {
    event.preventDefault();
    const message = agentInput.trim();
    if (!message || agentLoading) {
      return;
    }

    setAgentMessages((prev) => [...prev, { id: crypto.randomUUID(), role: "user", content: message }]);
    setAgentInput("");
    setAgentError("");

    try {
      setAgentLoading(true);
      const response = await fetch(AGENT_API, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId, message }),
      });
      if (!response.ok) {
        throw new Error("AIエージェントへの依頼に失敗しました");
      }
      const data = await response.json();
      setAgentMessages((prev) => [...prev, { id: crypto.randomUUID(), role: "assistant", content: data.message }]);
      await loadTodos();
    } catch (err) {
      setAgentError(err.message || "Unknown error");
    } finally {
      setAgentLoading(false);
    }
  };

  const sortedTodos = useMemo(
    () =>
      [...todos].sort((a, b) => {
        if (a.completed === b.completed) {
          return a.id - b.id;
        }
        return a.completed ? 1 : -1;
      }),
    [todos],
  );

  return (
    <div className="todo-app">
      <header>
        <h1>Quarkus TODO</h1>
        <p>
          バックエンドの <code>/api/todos</code> と連携する Web UI
        </p>
      </header>

      <div className="app-grid">
        <main>
          <section className="todo-form">
            <form onSubmit={handleSubmit}>
              <input
                name="title"
                type="text"
                placeholder="新しいタスクを入力"
                value={form.title}
                onChange={handleChange}
                disabled={submitting}
              />
              <label className="form-checkbox">
                <input
                  name="completed"
                  type="checkbox"
                  checked={form.completed}
                  onChange={handleChange}
                  disabled={submitting}
                />
                完了済みとして追加
              </label>
              <button type="submit" disabled={submitting}>
                {submitting ? "追加中..." : "タスクを追加"}
              </button>
            </form>
            <div className="todo-stats">
              <span>全件: {todos.length}</span>
              <span>未完了: {pendingCount}</span>
            </div>
          </section>

          {error && <p className="status status-error">{error}</p>}
          {loading ? (
            <p className="status">読み込み中...</p>
          ) : (
            <section className="todo-list">
              {sortedTodos.length === 0 ? (
                <p className="status">タスクはありません。追加してみましょう。</p>
              ) : (
                <ul>
                  {sortedTodos.map((todo) => (
                    <li key={todo.id} className={todo.completed ? "completed" : ""}>
                      <label>
                        <input type="checkbox" checked={todo.completed} onChange={() => toggleTodo(todo)} />
                        <span>{todo.title}</span>
                      </label>
                      <button type="button" className="delete-button" onClick={() => deleteTodo(todo.id)}>
                        削除
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          )}
        </main>

        <aside className="agent-panel">
          <div className="agent-heading">
            <div>
              <p className="agent-eyebrow">AI AGENT</p>
              <h2>Todoアシスタント</h2>
            </div>
            <span className="agent-status">オンライン</span>
          </div>
          <div className="agent-messages" aria-live="polite">
            {agentMessages.map((message) => (
              <p key={message.id} className={`agent-message ${message.role}`}>
                {message.content}
              </p>
            ))}
            {agentLoading && <p className="agent-message assistant agent-thinking">考えています...</p>}
          </div>
          {agentError && <p className="status status-error">{agentError}</p>}
          <form className="agent-form" onSubmit={handleAgentSubmit}>
            <textarea
              value={agentInput}
              onChange={(event) => setAgentInput(event.target.value)}
              placeholder="例: 未完了のタスクを教えて"
              maxLength={2000}
              disabled={agentLoading}
            />
            <button type="submit" disabled={agentLoading || !agentInput.trim()}>
              {agentLoading ? "依頼中..." : "依頼する"}
            </button>
          </form>
        </aside>
      </div>
    </div>
  );
}

export default App;
