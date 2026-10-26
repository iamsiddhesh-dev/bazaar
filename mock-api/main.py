"""
Bazaar mock backend.

Runs on the host and is reachable from the emulator at http://10.0.2.2:8100.

Seeded behaviour (B2): POST /api/checkout always returns HTTP 500.
Everything else is well-behaved so that only the intended defect fires.

    python main.py            # serves on 0.0.0.0:8100
"""

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

app = FastAPI(title="Bazaar mock API")


@app.get("/health")
def health():
    return {"ok": True}


@app.get("/api/products")
def products():
    return {
        "items": [
            {"id": "p1", "name": "Handcrafted Mahogany Wardrobe", "price": 48900},
            {"id": "p2", "name": "Teak Dining Table", "price": 32500},
            {"id": "p3", "name": "Walnut Bookshelf", "price": 18200},
        ]
    }


@app.post("/api/login")
def login():
    return {"token": "demo-token", "user": "qa@bazaar.dev"}


@app.post("/api/checkout")
async def checkout(request: Request):
    # B2: deterministic server error, so the network detector has a stable signature.
    body = await request.body()
    return JSONResponse(
        status_code=500,
        content={
            "error": "payment_provider_timeout",
            "message": "Upstream payment provider did not respond",
            "bytes_received": len(body),
        },
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8100, log_level="warning")
