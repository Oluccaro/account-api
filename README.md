# Account API

In-memory banking API: `POST /event`, `GET /balance`, `POST /reset`.

## Running

With Docker:

```
docker compose up --build
```

With a local JDK 21:

```
./mvnw spring-boot:run
```

```
./mvnw test
```

Either way the API listens on **port 3000**.

## Principal decisions

**Events are dispatched through a strategy.** `EventHandler` declares the `EventType` it serves;
`EventHandlerRegistry` indexes the injected handlers and refuses to start if a type has no handler
or more than one. Adding an operation is one class and one enum constant.

**Failures are returned, not thrown.** `TransactionResult` is a sealed interface, so the controller
matches it with an exhaustive `switch`. Account-not-found is an ordinary outcome, not an exception.
Exceptions are left for malformed JSON and validation.

**`Account` is immutable.** `deposit` and `withdraw` return new instances, so a handler computes
every new balance before writing any of them. A rejected transfer cannot leave a half-updated state.

**One lock.** `AccountService` is the single entry point and all its methods are `synchronized`,
reads included, so a transfer cannot interleave with another event or a balance read. Handlers are
written as if single-threaded.
