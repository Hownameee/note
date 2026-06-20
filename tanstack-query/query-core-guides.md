# TanStack Query React: Queries, Query Keys, Query Functions, And Query Options

This guide explains the core React Query pieces from the TanStack Query v5 docs:

- [Queries](https://tanstack.com/query/latest/docs/framework/react/guides/queries)
- [Query Keys](https://tanstack.com/query/latest/docs/framework/react/guides/query-keys)
- [Query Functions](https://tanstack.com/query/latest/docs/framework/react/guides/query-functions)
- [Query Options](https://tanstack.com/query/latest/docs/framework/react/guides/query-options)

It also uses related official docs for [Important Defaults](https://tanstack.com/query/latest/docs/framework/react/guides/important-defaults), [Caching](https://tanstack.com/query/latest/docs/framework/react/guides/caching), [`useQuery`](https://tanstack.com/query/latest/docs/framework/react/reference/useQuery), and [`QueryClient`](https://tanstack.com/query/latest/docs/reference/QueryClient).

## Mental Model

TanStack Query is a server-state manager.

Server state is data that lives outside the browser and can become stale without local user action: users, todos, projects, permissions, reports, search results, dashboards, and API responses.

TanStack Query does not replace local UI state like open modals, selected tabs, draft form values, or theme toggles. It manages asynchronous data by combining four ideas:

- `QueryClient`: the central object that owns the query cache and query defaults.
- `queryKey`: the stable identity of one cache entry.
- `queryFn`: the async function that fetches the data for that key.
- Query observer hooks such as `useQuery`: React subscriptions to cache entries.

The most common call is:

```tsx
const todosQuery = useQuery({
  queryKey: ['todos'],
  queryFn: fetchTodos,
})
```

Read it as:

> Subscribe this component to the cached data identified by `['todos']`. If the data is missing or stale and the query is allowed to fetch, run `fetchTodos`. Render again as the cache state changes.

## Minimal Setup

Create one `QueryClient` for the app, then provide it near the React root.

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      retry: 2,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
```

Use a stable client. Do not create `new QueryClient()` inside a component render body, because that creates a new cache repeatedly.

## Queries

A query is a declarative dependency on an asynchronous data source.

Use a query when:

- You are reading data from a server, file, worker, storage layer, or any promise-based source.
- The operation is repeatable and can be cached by identity.
- Components need loading, error, success, background refresh, retry, or stale state.
- Multiple components may need the same data.

Do not use a query for write operations that create, update, or delete server data. Use a mutation for those. A query function can technically use any promise, including a POST request, but if the operation modifies server state, model it as a mutation.

### Required Parts

`useQuery` needs at least:

- `queryKey`: a unique array that describes the data.
- `queryFn`: a function that returns a promise resolving the data or rejecting/throwing an error.

```tsx
import { useQuery } from '@tanstack/react-query'

type Todo = {
  id: number
  title: string
  completed: boolean
}

async function fetchTodos(): Promise<Todo[]> {
  const response = await fetch('/api/todos')

  if (!response.ok) {
    throw new Error('Failed to load todos')
  }

  return response.json()
}

export function TodoList() {
  const query = useQuery({
    queryKey: ['todos'],
    queryFn: fetchTodos,
  })

  if (query.isPending) {
    return <p>Loading...</p>
  }

  if (query.isError) {
    return <p>{query.error.message}</p>
  }

  return (
    <ul>
      {query.data.map((todo) => (
        <li key={todo.id}>{todo.title}</li>
      ))}
    </ul>
  )
}
```

### Primary Query States

`status` describes whether the query has usable data.

| State | Boolean | Meaning |
| --- | --- | --- |
| `pending` | `isPending` | There is no data yet. |
| `error` | `isError` | The query function failed. |
| `success` | `isSuccess` | Data is available. |

This pattern is enough for many components:

```tsx
if (query.isPending) return <Loading />
if (query.isError) return <ErrorView error={query.error} />
return <TodoTable todos={query.data} />
```

After the pending and error checks, TypeScript can narrow `data` to the successful data type.

### Fetch Status

`fetchStatus` describes whether the query function is running.

| Fetch status | Meaning |
| --- | --- |
| `fetching` | A request is currently running. |
| `paused` | The query wants to fetch but is paused, commonly due to network mode/offline state. |
| `idle` | No query function is running. |

`status` and `fetchStatus` answer different questions:

- `status`: do we have data?
- `fetchStatus`: is the query function currently executing?

That distinction matters because a query can have data and still be fetching in the background.

```tsx
function TodoPanel() {
  const query = useQuery({
    queryKey: ['todos'],
    queryFn: fetchTodos,
  })

  if (query.isPending) {
    return <p>First load...</p>
  }

  if (query.isError) {
    return <p>{query.error.message}</p>
  }

  return (
    <>
      {query.isFetching && <small>Refreshing...</small>}
      <TodoTable todos={query.data} />
    </>
  )
}
```

### `isPending`, `isLoading`, `isFetching`, And `isRefetching`

These names are easy to confuse.

| Flag | Use it when you mean |
| --- | --- |
| `isPending` | There is no data yet. This is usually the first full-page or component loading state. |
| `isFetching` | A fetch is happening now, including first fetch and background refetches. |
| `isRefetching` | A background fetch is happening after initial data already exists. |
| `isLoading` | Initial loading derived from pending plus fetching. Useful, but `isPending` is often clearer for data availability. |

Practical rendering pattern:

```tsx
if (query.isPending) {
  return <FullPageSpinner />
}

if (query.isError) {
  return <ErrorMessage error={query.error} />
}

return (
  <section>
    {query.isRefetching && <RefreshBadge />}
    <DataView data={query.data} />
  </section>
)
```

## Query Keys

Query keys are the identity layer for the cache. TanStack Query stores and looks up cached data by a stable hash of the `queryKey`.

Rules:

- The top-level query key must be an array.
- The key must be serializable.
- The key must uniquely describe the data returned by the query function.
- Every variable that changes the fetched data must be in the key.

### Simple Keys

Use simple keys for whole collections or non-parameterized data.

```tsx
useQuery({
  queryKey: ['todos'],
  queryFn: fetchTodos,
})

useQuery({
  queryKey: ['current-user'],
  queryFn: fetchCurrentUser,
})
```

Good for:

- Generic list resources.
- Current session/user data.
- Static reference data.
- API calls with no meaningful parameters.

### Keys With Variables

When the data depends on a value, include that value in the key.

```tsx
function TodoDetails({ todoId }: { todoId: number }) {
  return useQuery({
    queryKey: ['todos', todoId],
    queryFn: () => fetchTodo(todoId),
  })
}
```

If `todoId` changes from `1` to `2`, TanStack Query sees a different key:

- `['todos', 1]`
- `['todos', 2]`

Those are separate cache entries. The component moves from observing one entry to observing the other.

### Keys With Filter Objects

Use an object segment for named filters. This is easier to read and safer than positional arrays when there are multiple parameters.

```tsx
type TodoFilters = {
  status?: 'open' | 'done'
  page: number
  search?: string
}

function useTodos(filters: TodoFilters) {
  return useQuery({
    queryKey: ['todos', filters],
    queryFn: () => fetchTodos(filters),
  })
}
```

Good:

```tsx
['todos', { status: 'open', page: 1 }]
```

Harder to maintain:

```tsx
['todos', 'open', 1]
```

The object style gives each value a name.

### Deterministic Hashing

Object property order does not matter inside query keys. These keys are equivalent:

```tsx
['todos', { status: 'open', page: 1 }]
['todos', { page: 1, status: 'open' }]
```

Array item order does matter. These keys are different:

```tsx
['todos', 'open', 1]
['todos', 1, 'open']
```

Use this intentionally. Put stable hierarchy first, then identifiers, then filter objects:

```tsx
['projects']
['projects', projectId]
['projects', projectId, 'tasks']
['projects', projectId, 'tasks', { status, page }]
```

### Query Keys As Dependencies

Think of `queryKey` as the dependency array for `queryFn`.

If the query function depends on a variable, the key must include that variable.

Wrong:

```tsx
function UserProfile({ userId }: { userId: string }) {
  return useQuery({
    queryKey: ['user'],
    queryFn: () => fetchUser(userId),
  })
}
```

Why it is wrong:

- `userId` changes the returned data.
- The key stays `['user']`.
- Different users can overwrite the same cache entry.
- React Query cannot know when the identity changed.

Correct:

```tsx
function UserProfile({ userId }: { userId: string }) {
  return useQuery({
    queryKey: ['user', userId],
    queryFn: () => fetchUser(userId),
  })
}
```

### Key Design Guidelines

Use consistent key factories in real projects.

```tsx
export const userKeys = {
  all: ['users'] as const,
  lists: () => [...userKeys.all, 'list'] as const,
  list: (filters: UserFilters) => [...userKeys.lists(), filters] as const,
  details: () => [...userKeys.all, 'detail'] as const,
  detail: (id: string) => [...userKeys.details(), id] as const,
}
```

Then use them everywhere:

```tsx
useQuery({
  queryKey: userKeys.detail(userId),
  queryFn: () => fetchUser(userId),
})

queryClient.invalidateQueries({
  queryKey: userKeys.lists(),
})
```

Benefits:

- Fewer typos.
- Easier invalidation.
- Shared conventions between hooks, prefetching, and cache updates.
- Better TypeScript inference when combined with `queryOptions`.

## Query Functions

A query function is any function that returns a promise. The promise must:

- Resolve to data.
- Throw an error or return a rejected promise on failure.
- Never resolve `undefined` as successful data.

Use `null` when a successful response means "no value".

### Common Forms

Pass a named function when no variables are needed:

```tsx
useQuery({
  queryKey: ['todos'],
  queryFn: fetchTodos,
})
```

Use a closure when the query needs component props or state:

```tsx
useQuery({
  queryKey: ['todos', todoId],
  queryFn: () => fetchTodo(todoId),
})
```

Use the `QueryFunctionContext` when you want the function to derive variables from the key:

```tsx
useQuery({
  queryKey: ['todos', todoId],
  queryFn: ({ queryKey }) => {
    const [, id] = queryKey as ['todos', number]
    return fetchTodo(id)
  },
})
```

The closure form is often simpler in application code. The context form is useful when extracting reusable query functions or using a default query function.

### Handling Fetch Errors

The browser `fetch` API does not throw for HTTP 400 or 500 responses. It only rejects for network-level failures. You must check `response.ok`.

```tsx
async function fetchJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, { signal })

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }

  return response.json()
}

function useTodo(todoId: number) {
  return useQuery({
    queryKey: ['todos', todoId],
    queryFn: () => fetchJson<Todo>(`/api/todos/${todoId}`),
  })
}
```

Libraries like Axios usually reject on unsuccessful HTTP status codes automatically, but still check your client behavior.

### QueryFunctionContext

TanStack Query passes an object to the query function. Important fields:

| Field | Meaning |
| --- | --- |
| `queryKey` | The key used for this query. |
| `client` | The `QueryClient` instance. |
| `signal` | An `AbortSignal` for cancellation. |
| `meta` | Optional metadata from query options. |

Use `signal` to let TanStack Query cancel a request when it becomes obsolete.

```tsx
function useSearchTodos(search: string) {
  return useQuery({
    queryKey: ['todos', 'search', { search }],
    queryFn: ({ signal }) =>
      fetchJson<Todo[]>(`/api/todos?search=${encodeURIComponent(search)}`, signal),
  })
}
```

If the component changes from `search="a"` to `search="ab"` quickly, the older request can be aborted if your fetcher passes the signal to `fetch`.

### Returning Data

Return the exact data shape the rest of the app expects.

Good:

```tsx
async function fetchTodo(id: number): Promise<Todo | null> {
  const response = await fetch(`/api/todos/${id}`)

  if (response.status === 404) {
    return null
  }

  if (!response.ok) {
    throw new Error('Failed to load todo')
  }

  return response.json()
}
```

Avoid:

```tsx
async function fetchTodo(id: number) {
  const response = await fetch(`/api/todos/${id}`)
  return response.json().catch(() => undefined)
}
```

Resolving `undefined` makes the query invalid. If no data is a valid result, return `null`, an empty array, or a domain-specific value.

## Query Options

`queryOptions` is a helper for grouping `queryKey`, `queryFn`, and default options in one reusable function.

At runtime, `queryOptions(...)` returns the same object you pass in. The value is mostly in TypeScript inference and consistency.

```tsx
import { queryOptions } from '@tanstack/react-query'

function todoOptions(todoId: number) {
  return queryOptions({
    queryKey: ['todos', todoId],
    queryFn: () => fetchTodo(todoId),
    staleTime: 60_000,
  })
}
```

Use it with hooks:

```tsx
function TodoScreen({ todoId }: { todoId: number }) {
  const query = useQuery(todoOptions(todoId))

  if (query.isPending) return <p>Loading...</p>
  if (query.isError) return <p>{query.error.message}</p>

  return <TodoDetails todo={query.data} />
}
```

Use it with the `QueryClient`:

```tsx
await queryClient.prefetchQuery(todoOptions(42))

const cachedTodo = queryClient.getQueryData(todoOptions(42).queryKey)

queryClient.setQueryData(todoOptions(42).queryKey, {
  id: 42,
  title: 'Updated locally',
  completed: false,
})
```

### Why Use `queryOptions`

Use `queryOptions` when:

- The same query is used in multiple components.
- You prefetch the same query before rendering it.
- You read or update the same cache entry imperatively.
- You want one canonical place for `queryKey`, `queryFn`, `staleTime`, and common options.
- You want TypeScript to connect the key with the query function result.

Without `queryOptions`, it is easy to duplicate options and accidentally drift:

```tsx
useQuery({
  queryKey: ['todo', todoId],
  queryFn: () => fetchTodo(todoId),
})

queryClient.prefetchQuery({
  queryKey: ['todos', todoId],
  queryFn: () => fetchTodo(todoId),
})
```

The two keys differ: `['todo', todoId]` vs `['todos', todoId]`. That creates two separate cache entries.

With `queryOptions`, both call sites reuse the same identity:

```tsx
useQuery(todoOptions(todoId))
queryClient.prefetchQuery(todoOptions(todoId))
```

### Per-Component Overrides

You can spread shared options and override component-specific behavior.

```tsx
function TodoTitle({ todoId }: { todoId: number }) {
  const query = useQuery({
    ...todoOptions(todoId),
    select: (todo) => todo.title,
  })

  if (query.isPending) return <p>Loading...</p>
  if (query.isError) return <p>{query.error.message}</p>

  return <h1>{query.data}</h1>
}
```

`select` transforms the data observed by this component. It does not change the cached raw data.

Use this for small, component-local projections:

- Count items.
- Pick a field.
- Sort a small list for presentation.
- Convert API data into a view model for one component.

Avoid heavy transformations in `select` if they run often. Move expensive work to the server, the query function, memoized selectors, or normalized domain utilities.

## Under The Hood

This is the practical lifecycle behind `useQuery`.

### First Mount With Empty Cache

Code:

```tsx
useQuery({
  queryKey: ['todos'],
  queryFn: fetchTodos,
})
```

Flow:

1. React renders the component.
2. `useQuery` asks the nearest `QueryClient` for the cache entry matching `['todos']`.
3. No cache entry exists, so TanStack Query creates one.
4. A query observer subscribes the component to that cache entry.
5. Because the query has no data and is enabled, TanStack Query runs `fetchTodos`.
6. The hook returns a pending result. The component renders loading UI.
7. The promise resolves.
8. The cache entry stores the data and timestamps such as `dataUpdatedAt`.
9. Observers are notified.
10. React re-renders subscribed components with `status: 'success'`.

### Second Component Mounts With Same Key

If another component uses the same key:

```tsx
useQuery({
  queryKey: ['todos'],
  queryFn: fetchTodos,
})
```

Flow:

1. The observer subscribes to the existing `['todos']` cache entry.
2. Cached data is returned immediately.
3. If the data is stale, a background refetch may start.
4. Both components observe the same cache entry.
5. When fresh data arrives, both components receive the update.

This is why query keys matter. The key is the sharing boundary.

### Stale Data

By default, cached query data is considered stale immediately (`staleTime: 0`).

Stale does not mean deleted or unusable. It means:

- TanStack Query may refetch it when a refetch trigger happens.
- The UI can still render the stale cached data while the background fetch runs.

Common refetch triggers for stale queries:

- A new observer mounts.
- The window is refocused.
- The network reconnects.
- Manual invalidation happens.
- A configured interval fires.

Set `staleTime` when the data can stay fresh for a known period.

```tsx
useQuery({
  queryKey: ['countries'],
  queryFn: fetchCountries,
  staleTime: 24 * 60 * 60_000,
})
```

Use `staleTime: Infinity` for data that should not refetch due to staleness but can still be invalidated manually.

Use `staleTime: 'static'` for data that should never refetch while the app is running, even after invalidation.

### Inactive Queries And Garbage Collection

A query is active while at least one observer is subscribed. A component using `useQuery` is one observer.

When the last observer unmounts:

1. The query becomes inactive.
2. Data remains in the cache.
3. A garbage collection timer starts.
4. By default, inactive query data is removed after 5 minutes.

The option is `gcTime`.

```tsx
useQuery({
  queryKey: ['large-report', reportId],
  queryFn: () => fetchLargeReport(reportId),
  gcTime: 60_000,
})
```

Use shorter `gcTime` for large, rarely reused data. Use longer `gcTime` for data the user is likely to navigate back to.

### Retry

On the client, failed queries retry by default. The default is 3 attempts with backoff.

Disable retry for errors that should not be retried:

```tsx
useQuery({
  queryKey: ['invoice', invoiceId],
  queryFn: () => fetchInvoice(invoiceId),
  retry: (failureCount, error) => {
    if (error instanceof NotFoundError) {
      return false
    }

    return failureCount < 2
  },
})
```

Retry is useful for temporary network or server failures. It is harmful when it hides permanent errors or hammers an endpoint.

### Structural Sharing

By default, TanStack Query structurally shares JSON-compatible response data.

If a refetch returns data that is deeply equal in many places, TanStack Query preserves unchanged object references where it can. This helps `useMemo`, `useCallback`, and memoized child components avoid unnecessary work.

You usually do not need to configure this. Be aware of it when debugging object identity.

## Full Demo: Todo Queries

This example shows query keys, query functions, query options, prefetching, loading states, and per-component `select`.

### API Types

```tsx
export type Todo = {
  id: number
  title: string
  completed: boolean
}

export type TodoFilters = {
  status?: 'all' | 'open' | 'done'
  page: number
}
```

### Fetch Helper

```tsx
async function fetchJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, { signal })

  if (!response.ok) {
    throw new Error(`Request failed with ${response.status}`)
  }

  return response.json()
}
```

### Query Keys

```tsx
export const todoKeys = {
  all: ['todos'] as const,
  lists: () => [...todoKeys.all, 'list'] as const,
  list: (filters: TodoFilters) => [...todoKeys.lists(), filters] as const,
  details: () => [...todoKeys.all, 'detail'] as const,
  detail: (id: number) => [...todoKeys.details(), id] as const,
}
```

### Query Options

```tsx
import { queryOptions } from '@tanstack/react-query'

export function todoListOptions(filters: TodoFilters) {
  return queryOptions({
    queryKey: todoKeys.list(filters),
    queryFn: ({ signal }) => {
      const params = new URLSearchParams()

      params.set('page', String(filters.page))

      if (filters.status && filters.status !== 'all') {
        params.set('status', filters.status)
      }

      return fetchJson<Todo[]>(`/api/todos?${params}`, signal)
    },
    staleTime: 30_000,
  })
}

export function todoDetailOptions(id: number) {
  return queryOptions({
    queryKey: todoKeys.detail(id),
    queryFn: ({ signal }) => fetchJson<Todo>(`/api/todos/${id}`, signal),
    staleTime: 60_000,
  })
}
```

### List Component

```tsx
import { useQuery, useQueryClient } from '@tanstack/react-query'

type TodoListProps = {
  filters: TodoFilters
  onOpenTodo: (todoId: number) => void
}

export function TodoList({ filters, onOpenTodo }: TodoListProps) {
  const queryClient = useQueryClient()
  const query = useQuery(todoListOptions(filters))

  if (query.isPending) {
    return <p>Loading todos...</p>
  }

  if (query.isError) {
    return <p>{query.error.message}</p>
  }

  return (
    <section>
      {query.isRefetching && <p>Refreshing...</p>}

      <ul>
        {query.data.map((todo) => (
          <li key={todo.id}>
            <button
              type="button"
              onClick={() => onOpenTodo(todo.id)}
              onMouseEnter={() => {
                queryClient.prefetchQuery(todoDetailOptions(todo.id))
              }}
            >
              {todo.title}
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
```

What happens:

- `todoListOptions(filters)` creates a key containing every filter that changes the response.
- The first render shows loading if no cached data exists.
- A later render can show cached data while `isRefetching` is true.
- Hovering a todo prefetches the detail query before navigation.

### Detail Component

```tsx
export function TodoDetail({ todoId }: { todoId: number }) {
  const query = useQuery(todoDetailOptions(todoId))

  if (query.isPending) {
    return <p>Loading todo...</p>
  }

  if (query.isError) {
    return <p>{query.error.message}</p>
  }

  return (
    <article>
      <h1>{query.data.title}</h1>
      <p>{query.data.completed ? 'Done' : 'Open'}</p>
    </article>
  )
}
```

### Count Component With `select`

```tsx
export function OpenTodoCount({ filters }: { filters: TodoFilters }) {
  const query = useQuery({
    ...todoListOptions(filters),
    select: (todos) => todos.filter((todo) => !todo.completed).length,
  })

  if (query.isPending) {
    return <span>...</span>
  }

  if (query.isError) {
    return <span>?</span>
  }

  return <span>{query.data}</span>
}
```

The cache still stores `Todo[]`. This component only observes a derived number.

## When To Use Each Piece

| Piece | Use when | Avoid when |
| --- | --- | --- |
| `useQuery` | A React component needs server data and should react to loading, error, success, and refetch state. | You are writing data to the server. Use `useMutation`. |
| `queryKey` | You need to identify cache data, separate parameters, share data, invalidate data, or prefetch data. | Never omit variables that affect the result. |
| `queryFn` | You need to fetch promise-based data for a query key. | Do not return `undefined`; do not swallow errors that should be visible. |
| `queryOptions` | You reuse the same query in hooks, prefetching, cache reads, or cache writes. | One-off tiny queries can be inline if they are not reused. |
| `staleTime` | You know how long cached data can be trusted before background refetch. | Do not set everything to `Infinity` just to stop refetches. |
| `gcTime` | You need to control memory retention after queries become inactive. | Do not confuse it with freshness; it controls deletion, not staleness. |
| `select` | A component needs a small derived view of cached data. | Avoid expensive repeated transformations. |

## Common Mistakes

### Missing Variables In The Key

```tsx
useQuery({
  queryKey: ['projects'],
  queryFn: () => fetchProjects({ orgId }),
})
```

Fix:

```tsx
useQuery({
  queryKey: ['projects', { orgId }],
  queryFn: () => fetchProjects({ orgId }),
})
```

### Treating `isFetching` As First Loading Only

`isFetching` is true for background refetches too. Use `isPending` for "no data yet".

### Forgetting `response.ok`

```tsx
async function fetchTodos() {
  const response = await fetch('/api/todos')
  return response.json()
}
```

Fix:

```tsx
async function fetchTodos() {
  const response = await fetch('/api/todos')

  if (!response.ok) {
    throw new Error('Failed to load todos')
  }

  return response.json()
}
```

### Creating Multiple Query Clients Accidentally

Wrong:

```tsx
function App() {
  const queryClient = new QueryClient()

  return (
    <QueryClientProvider client={queryClient}>
      <Routes />
    </QueryClientProvider>
  )
}
```

Create the client outside render or lazily in state.

```tsx
const queryClient = new QueryClient()

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Routes />
    </QueryClientProvider>
  )
}
```

### Using `gcTime` To Control Refetching

`gcTime` does not make data fresh. It controls how long inactive data stays in memory.

Use `staleTime` to reduce refetches.

```tsx
useQuery({
  queryKey: ['settings'],
  queryFn: fetchSettings,
  staleTime: 5 * 60_000,
  gcTime: 30 * 60_000,
})
```

## Practical Checklist

When adding a query:

1. Name the data being fetched.
2. Design the query key from stable hierarchy plus every variable that changes the result.
3. Write a query function that returns data, throws on error, and respects `AbortSignal` when possible.
4. Decide `staleTime` based on how often the data changes.
5. Decide whether the query should be inline or extracted with `queryOptions`.
6. Render `isPending`, `isError`, and success states.
7. Use `isFetching` or `isRefetching` only for background activity indicators.
8. Use `queryClient.invalidateQueries` after mutations that make related cached data stale.

## Summary

TanStack Query works by making server data addressable. The `queryKey` names the data, the `queryFn` fetches it, the `QueryClient` stores it, and `useQuery` subscribes React components to its lifecycle.

Use queries for reads. Put every data-changing variable in the key. Make query functions throw on failure and resolve real values on success. Extract repeated query definitions with `queryOptions` so hooks, prefetching, and cache operations all share the same identity.
