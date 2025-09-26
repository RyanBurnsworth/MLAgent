import json
import pandas as pd
import pytest

from services.dataset_service import DatasetService


@pytest.fixture
def dataset_service(tmp_path, monkeypatch):
    # Patch the WORKDIR to use pytest tmp_path
    monkeypatch.setattr("services.dataset_service.WORKDIR", tmp_path)
    return DatasetService()


def test_list_datasets_returns_csvs(dataset_service, tmp_path):
    dataset_dir = tmp_path / "my_dataset"
    dataset_dir.mkdir()
    f1 = dataset_dir / "train.csv"
    f2 = dataset_dir / "test.csv"
    f1.write_text("a,b,c\n1,2,3\n")
    f2.write_text("x,y,z\n4,5,6\n")

    result = dataset_service.list_datasets(dataset_dir)

    assert len(result) == 2
    assert all(p.suffix == ".csv" for p in result)


def test_read_csv_reads_file(dataset_service, tmp_path):
    dataset_dir = tmp_path / "my_dataset"
    dataset_dir.mkdir()
    csv_file = dataset_dir / "train.csv"
    csv_file.write_text("col1,col2\n1,2\n3,4\n")

    df = dataset_service.read_csv("my_dataset", "train")
    assert isinstance(df, pd.DataFrame)
    assert list(df.columns) == ["col1", "col2"]
    assert df.shape == (2, 2)


def test_get_headers(dataset_service, tmp_path):
    dataset_dir = tmp_path / "my_dataset"
    dataset_dir.mkdir()
    (dataset_dir / "train.csv").write_text("col1,col2,col3\n1,2,3\n")

    headers = dataset_service.get_headers("my_dataset", "train")
    assert headers == ["col1", "col2", "col3"]


def test_get_top_25_rows(dataset_service, tmp_path):
    dataset_dir = tmp_path / "my_dataset"
    dataset_dir.mkdir()
    data = "\n".join([f"{i},{i+1}" for i in range(30)])
    (dataset_dir / "train.csv").write_text("a,b\n" + data)

    top25 = dataset_service.get_top_25_rows("my_dataset", "train")
    assert len(top25.splitlines()) == 25  # 25 rows only
    assert "0,1" in top25


def test_get_dataset_manifest_reads_and_parses(monkeypatch, dataset_service, tmp_path):
    manifest_file = tmp_path / "dataset-metadata.json"
    content = json.dumps({"title": "Demo", "subtitle": "Sub", "description": "Desc"})
    manifest_file.write_text(json.dumps(content))  # double-encoded

    # Patch subprocess.run to do nothing
    monkeypatch.setattr("subprocess.run", lambda *a, **k: None)

    # Patch open path
    monkeypatch.chdir(tmp_path)

    result = dataset_service.get_dataset_manifest("demo/dataset")
    assert result["title"] == "Demo"
    assert result["subtitle"] == "Sub"
    assert result["description"] == "Desc"


def test_get_dataset_details(monkeypatch, dataset_service, tmp_path):
    dataset_dir = tmp_path / "demo_dataset"
    dataset_dir.mkdir()
    (dataset_dir / "train.csv").write_text("col1,col2\n1,2\n")

    # Patch helper methods
    monkeypatch.setattr(dataset_service, "get_dataset_manifest", lambda x: {
        "title": "DemoTitle", "subtitle": "DemoSub", "description": "DemoDesc"
    })

    record = dataset_service.get_dataset_details("demo_dataset", "train")
    assert record["title"] == "DemoTitle"
    assert record["datasets"] == ["train"]
    assert "col1" in record["headers"]
    assert "1,2" in record["top25"]
