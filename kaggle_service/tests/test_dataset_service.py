import pytest
import json
from unittest.mock import patch, MagicMock
from pathlib import Path
import subprocess

from services.dataset_service import DatasetService

@pytest.fixture
def dataset_service(tmp_path):
    # Use tmp_path for isolated test directory
    service = DatasetService()
    service.workdir = tmp_path
    return service

def test_download_dataset_success(dataset_service):
    """
    Test successful dataset download flow.
    """
    mock_output = "ref title size\n--- --- ---\ndataset1 Dataset One 100KB"

    with patch("services.dataset_service.subprocess.run") as mock_subprocess, \
         patch.object(DatasetService, "list_datasets") as mock_list, \
         patch.object(DatasetService, "get_dataset_details") as mock_details:

        mock_subprocess.return_value = MagicMock(stdout=mock_output, returncode=0)
        mock_list.return_value = [Path("dummy.csv")]
        mock_details.return_value = {"dataset_name": "dataset1"}

        result = dataset_service.download_dataset("ufo")

        assert result["dataset_name"] == "dataset1"

def test_download_dataset_no_output(dataset_service):
    """
    Test download_dataset handles no datasets found properly.
    """
    mock_output = "ref title size\n--- --- ---\n"  # headers only

    with patch("services.dataset_service.subprocess.run") as mock_subprocess:
        mock_subprocess.return_value = MagicMock(stdout=mock_output, returncode=0)

        result = dataset_service.download_dataset("nonexistent")
        assert result == ""

def test_download_dataset_calledprocesserror(dataset_service):
    """
    Test that download_dataset handles subprocess errors gracefully.
    """
    with patch("services.dataset_service.subprocess.run") as mock_subprocess:
        mock_subprocess.side_effect = subprocess.CalledProcessError(1, "kaggle")

        result = dataset_service.download_dataset("ufo")
        assert result is None

def test_get_dataset_manifest_success(dataset_service):
    """
    Test successful retrieval of dataset manifest.
    """
    mock_data = {
        "title": "UFO Sightings",
        "subtitle": "Sub",
        "description": "Desc"
    }

    with patch("services.dataset_service.subprocess.run") as mock_subprocess, \
         patch("builtins.open", new_callable=MagicMock) as mock_open:

        mock_subprocess.return_value = MagicMock()
        # Kaggle metadata file returns JSON as string
        mock_open.return_value.__enter__.return_value.read.return_value = json.dumps(mock_data)

        result = dataset_service.get_dataset_manifest("ufo")
        assert result["title"] == "UFO Sightings"

def test_get_dataset_manifest_subprocess_error(dataset_service):
    """
    Test get_dataset_manifest handles subprocess errors.
    """
    with patch("services.dataset_service.subprocess.run") as mock_subprocess:
        mock_subprocess.side_effect = subprocess.CalledProcessError(1, "cmd")

        result = dataset_service.get_dataset_manifest("ufo")
        assert result is None

def test_list_datasets_success(dataset_service, tmp_path):
    """
    Test list_datasets finds CSV files in directory.
    """
    csv_file = tmp_path / "file.csv"
    csv_file.write_text("test")

    result = dataset_service.list_datasets(tmp_path)
    assert csv_file in result

def test_list_datasets_no_csv(dataset_service, tmp_path):
    """
    Test list_datasets returns None when no CSV files exist.
    """
    result = dataset_service.list_datasets(tmp_path)
    assert result is None
