# miogram-plugin-sdk (Rust)

Rust SDK для WASM-плагинов Miogram. Компилируется в `wasm32-unknown-unknown`
и запускается в песочнице WAMR внутри клиента.

> Проєкт **Miogram / Міограм** · автор **@dkramochka**

## Быстрый старт

```toml
# Cargo.toml плагина
[dependencies]
miogram-plugin-sdk = { path = "path/to/miogram-plugin-sdk" }

[profile.release]
panic = "abort"
opt-level = "z"
```

```rust
use miogram_plugin_sdk::{register, Plugin};

#[derive(Default)]
struct Echo;

impl Plugin for Echo {
    fn handle(&mut self, op: &str, payload: &[u8]) -> Result<Vec<u8>, i32> {
        Ok(payload.to_vec()) // эхо для любой операции
    }
}

register!(Echo);
```

Сборка:

```bash
cargo build --target wasm32-unknown-unknown --release
```

Полученный `.wasm` подписывается Ed25519-манифестом (`PluginManifestCodec`)
и устанавливается через движок плагинов хоста.

## Контракт с хостом

Гость обязан экспортировать:

| Экспорт | Сигнатура | Назначение |
|---|---|---|
| `miogram_abi_version` | `() -> i32` | ревизия ABI (= 1) |
| `miogram_alloc` | `(len) -> ptr` | буфер из кучи плагина |
| `miogram_guest_free` | `(ptr, len)` | освобождение таких буферов |
| `miogram_call` | `(ptr, len) -> i64` | `(resp_ptr << 32) \| len`, `-1` при ошибке |

Хост выделяет запрос и ответ **только** через аллокатор плагина — смешивать
его с heap'ом WAMR нельзя. SDK реализует всё это автоматически; автору
доступен единственный метод [`Plugin::handle`] с уже разобранной парой
`(op, payload)`.

Формат фрейма — см. `src/envelope.rs` (`HYPR`, v1): нулевые зависимости
внутри гостя. Переход на FlatBuffers (`host_api.fbs`) запланирован вместе с
генерируемыми биндингами; конвертация тривиальна.

## Память и паники

* `panic = "abort"` обязателен в release: развёртывание сквозь FFI запрещено;
  abort виден хосту как trap → плагин карантинится штатной механикой.
* Бюджеты памяти/времени задаются `SandboxConfig` на стороне хоста.
* Все вызовы сериализованы (один exec_env на инстанс).

## Тесты

`cargo test` гоняет юнит-тесты envelope на хосте; CI собирает
`wasm32-unknown-unknown` для проверки линковки.
