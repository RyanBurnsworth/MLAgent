import pytest
import shutil
import json
from unittest.mock import patch, MagicMock
from services.notebook_service import NotebookService
from nbformat import v4, NotebookNode

@pytest.fixture
def notebook_service():
    return NotebookService("user", "test_notebook")


def test_create_or_append_notebook_append(tmp_path, notebook_service):
    # create a dummy notebook file
    notebook_file = notebook_service.WORKDIR / f"{notebook_service.NOTEBOOK_NAME}.ipynb"
    notebook_file.write_text(json.dumps({"cells": []}))
    cell = json.dumps({"cell_type": "code", "source": "print('hello')"})
    
    result = notebook_service.create_or_append_notebook(cell)
    assert result is True


def test_create_or_append_notebook_new(tmp_path):
    service = NotebookService("user", "new_notebook")
    cell = json.dumps({"cell_type": "code", "source": "print('hello')"})
    
    # simulate read_notebook returning None
    with patch.object(NotebookService, "read_notebook", return_value=None):
        result = service.create_or_append_notebook(cell)
        assert result is False


def test_write_to_notebook(tmp_path, notebook_service):
    notebook = {"cells": []}
    notebook_path = tmp_path / "nb.ipynb"
    result = notebook_service.write_to_notebook(notebook, notebook_path)
    assert result is True
    assert notebook_path.exists()


def test_read_notebook(tmp_path, notebook_service):
    notebook_path = notebook_service.WORKDIR / f"{notebook_service.NOTEBOOK_NAME}.ipynb"
    notebook_path.write_text(json.dumps({"cells": []}))
    result = notebook_service.read_notebook(notebook_service.NOTEBOOK_NAME)
    assert "cells" in result


def test_revert_notebook_with_backup(tmp_path, notebook_service):
    notebook_file = notebook_service.WORKDIR / f"{notebook_service.NOTEBOOK_NAME}.ipynb"
    backup_file = notebook_service.WORKDIR / f"{notebook_service.NOTEBOOK_NAME}-backup.ipynb"
    notebook_file.write_text("original")
    shutil.copy(notebook_file, backup_file)
    notebook_file.write_text("modified")
    notebook_service.revert_notebook()
    assert notebook_file.read_text() == "original"
    assert not backup_file.exists()


def test_create_metadata(tmp_path, notebook_service):
    result = notebook_service.create_metadata()
    assert result is True
    assert notebook_service.METADATA_PATH.exists()


@patch("nbformat.read")
def test_get_last_notebook_output_stream(mock_nb_read):
    # mock a NotebookNode with proper structure
    mock_nb_read.return_value = NotebookNode(cells=[
        NotebookNode(cell_type='code', outputs=[NotebookNode(output_type='stream', text='hello')])
    ])
    service = NotebookService("user", "test")
    output = service.get_last_notebook_output("dummy.ipynb")
    assert output == "hello"
