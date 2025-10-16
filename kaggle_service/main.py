from typing import Any, Dict, List
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from services.dataset_service import DatasetService
from services.notebook_service import NotebookService
from pydantic import BaseModel

app = FastAPI()

class CreateNotebookRequest(BaseModel):
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
    Create a notebook and test it.
"""
@app.post("/notebook/create/{notebook_name}")
def create_notebook(notebook_name: str, request: CreateNotebookRequest):
    statusDetails = ""

    try:
        notebook_service = NotebookService("ryanburnsworth", notebook_name)

        is_complete, tb_str = notebook_service.create_notebook(request.notebook_content)
        if isinstance(is_complete, Exception):
            statusDetails = tb_str
            raise is_complete

        is_success, last_output, tb_str = notebook_service.test_notebook()
        if is_success is False and isinstance(last_output, Exception):
            statusDetails = str(tb_str)
            raise last_output

    except Exception as e:
        return JSONResponse(
            status_code = 500,
            content = {
                "status": "error", 
                "message": str(e), 
                "details": statusDetails
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


"""
    Append to an existing notebook and test it.
"""
@app.post("/notebook/update/{notebook_name}")
def update_notebook(notebook_name: str,  cell_contents: List[Dict[str, Any]]):
    statusDetails = ""

    try:
        notebook_service = NotebookService("ryanburnsworth", notebook_name)

        is_complete, tb_str = notebook_service.append_cells_to_notebook(cell_contents)
        if isinstance(is_complete, Exception):
            statusDetails = tb_str
            raise is_complete

        is_success, last_output, tb_str = notebook_service.test_notebook()
        if is_success is False and isinstance(last_output, Exception):
            statusDetails = str(tb_str)
            raise last_output

    except Exception as e:
        return JSONResponse(
            status_code = 500,
            content = {
                "status": "error", 
                "message": str(e), 
                "details": statusDetails
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
