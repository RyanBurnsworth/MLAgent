import json
import subprocess
import pytest
from services.notebook_service import NotebookService


@pytest.fixture
def notebook_service(tmp_path, monkeypatch):
    """Fixture to create NotebookService with temp working dir."""
    # Force WORKDIR inside the module to point to tmp_path
    monkeypatch.setattr("services.notebook_service.Path", lambda p: tmp_path / p)

    svc = NotebookService("testuser", "testnb")
    return svc


def test_init_creates_workdir_and_metadata(tmp_path):
    svc = NotebookService("testuser", "testnb")

    # Check that workdir is created
    assert svc.WORKDIR.exists()
    assert svc.WORKDIR.is_dir()

    # Check that metadata file is created with correct contents
    metadata_file = svc.METADATA_PATH
    assert metadata_file.exists()

    data = json.loads(metadata_file.read_text())
    assert data["id"] == "testuser/testnb"
    assert data["title"] == "testnb"
    assert data["code_file"] == "testnb.ipynb"
    assert data["language"] == "python"


def test_create_notebook(tmp_path):
    svc = NotebookService("testuser", "testnb")

    notebook_content = {"cells": [], "metadata": {}, "nbformat": 4, "nbformat_minor": 5}
    svc.create_notebook(notebook_content)

    notebook_path = svc.WORKDIR / svc.NOTEBOOK_FILE
    assert notebook_path.exists()

    written = json.loads(notebook_path.read_text())
    assert written["nbformat"] == 4


def test_push_to_kaggle_success(monkeypatch):
    svc = NotebookService("user", "nb")

    calls = {"status": 0}

    def fake_run(args, **kwargs):
        # Simulate kaggle push returning success
        if "push" in args:
            return subprocess.CompletedProcess(args, 0)
        if "status" in args:
            if calls["status"] == 0:
                calls["status"] += 1
                return subprocess.CompletedProcess(args, 0, stdout="running")
            else:
                return subprocess.CompletedProcess(args, 0, stdout="complete")
        if "output" in args:
            return subprocess.CompletedProcess(args, 0)
        raise ValueError("Unexpected args", args)

    monkeypatch.setattr(subprocess, "run", fake_run)

    result = svc.push_to_kaggle()
    assert result is None  # push_to_kaggle prints but doesn’t return anything


def test_push_to_kaggle_failure(monkeypatch):
    svc = NotebookService("user", "nb")

    def fake_run(args, **kwargs):
        if "push" in args:
            raise subprocess.CalledProcessError(1, args, "fail")
        return subprocess.CompletedProcess(args, 0)

    monkeypatch.setattr(subprocess, "run", fake_run)

    result = svc.push_to_kaggle()
    assert result is None  # should handle failure gracefully
