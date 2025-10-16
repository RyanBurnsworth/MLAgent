from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from services.dataset_service import DatasetService
from services.notebook_service import NotebookService
from pydantic import BaseModel

app = FastAPI()

class NotebookUpdateRequest(BaseModel):
    notebook_content: dict

"""
    Download a dataset from Kaggle by providing a search term.
"""
@app.get("/dataset/download/{search_term}")
def download_dataset(search_term: str):
    dataset_service = DatasetService()

    try:
        result = dataset_service.download_dataset(search_term)
        return result
    except Exception as e:
        return JSONResponse(
            status_code = 500,
            content = {
                "status": "error", 
                "message": "Error downloading dataset.", 
                "details": str(e)
            }
        )

"""
    Create or append to a Kaggle notebook and test it.
"""
@app.post("/notebook/update/{notebook_name}")
def update_notebook(notebook_name: str, request: NotebookUpdateRequest):
    try:
        notebook_service = NotebookService("ryanburnsworth", notebook_name)

        is_complete = notebook_service.create_or_append_notebook(request.notebook_content)
        if isinstance(is_complete, Exception):
            raise is_complete

        is_tested = notebook_service.test_notebook()
        if isinstance(is_tested, Exception):
            raise is_tested

    except Exception as e:
        return JSONResponse(
            status_code = 500,
            content = {
                "status": "error", 
                "message": "Error updating or testing notebook.", 
                "details": str(e)
            }
        )
    
    return JSONResponse(
        status_code = 200,
        content = {
            "status": "success", 
            "message": "", 
            "details": ""
        }
    )



@app.exception_handler(RequestValidationError)
async def exception_handler(request: Request, exc: RequestValidationError):
    print(f"Unhandled exception: {exc}")
    
    return JSONResponse(
        status_code=500,
        content={
            "status": "error",
            "message": "An unexpected error occurred",
            "details": str(exc)  
        },
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
