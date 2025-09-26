import json
from pathlib import Path
import pandas as pd
import subprocess

WORKDIR = Path(f"./datasets")

class DatasetService:


    def __init__(self):
        self.workdir = WORKDIR
        self.workdir.mkdir(exist_ok=True)


    """
    
    Download the dataset matching the search term to the specified path
    
    """
    def download_dataset(self, search_term):
        cmd = [
            "kaggle", "datasets", "list",
            "--search", f"{search_term}",
            "--sort-by", "hottest"
        ]

        try:
            result = subprocess.run(cmd, capture_output=True, text=True, check=True)
            output = result.stdout

            if (output.lower == "no datasets found"):
                print("No datasets found.")
                return ""

            lines = output.strip().split("\n")

            data_lines = lines[2:]

            first_line = data_lines[0]

            dataset_name = first_line.split()[0]

            print("Dataset name:", dataset_name)

            download_path = Path(self.workdir) / dataset_name
            Path(download_path).mkdir(parents=True, exist_ok=True)

            subprocess.run(
                ["kaggle", "datasets", "download", dataset_name, "-p", download_path, "--unzip"],
                check=True
            )

            print(f"Dataset downloaded to {download_path}")

            datasets_paths = self.list_datasets(Path(download_path))

            # get the dataset names without the .csv extension
            datasets = [p.stem for p in datasets_paths]

            print(f"Datasets downloaded: {datasets} ")
        
            # return the dataset name and a list of dataset dataset file names
            return {"dataset_name": dataset_name, "datasets": datasets}
        except subprocess.CalledProcessError as e:
            print("An error occurred while downloading the dataset:", e)
            return None


    """
    
    Get detailed information about the dataset including title, description, datasets, headers, and top 25 rows
    
    """
    def get_dataset_details(self, dataset_name, training_data_filename):
        try:
            # extract the dataset details
            data = self.get_dataset_manifest(dataset_name)

            datasets_path = Path(self.workdir) / dataset_name
            datasets_paths = self.list_datasets(Path(datasets_path))

            # get the dataset names without the .csv extension
            datasets = [p.stem for p in datasets_paths]

            headers = self.get_headers(dataset_name, training_data_filename)
            top25 = self.get_top_25_rows(dataset_name, training_data_filename)

            record = {
                "title": data.get("title", ""),
                "subtitle": data.get("subtitle", ""),
                "description": data.get("description", ""),
                "datasets": datasets,
                "headers": headers,
                "top25": top25
            }

            print(f"Dataset details: {record}")

            return record
        except Exception as e:
            print("An error occurred while getting dataset details:", e)
            return None


    """
    
    Get the dataset manifest as a dictionary
    
    """
    def get_dataset_manifest(self, dataset_name):
        try:
            # downloads the datasets metadata
            subprocess.run(
                ["kaggle", "datasets", "metadata", dataset_name],
                check=True
            )

            with open("./dataset-metadata.json", "r", encoding="utf-8") as f:
                raw = f.read().strip()

            # Decode twice (since the file has JSON stored as a string)
            data = json.loads(raw)
            if isinstance(data, str):
                data = json.loads(data)
                return data
            
            return None
        except Exception as e:
            print("An error occurred while getting dataset manifest:", e)
            raise e


    """
    
    Get the headers of the dataset as a list of strings
    
    """
    def get_headers(self, dataset_name, training_data_filename):
        try:
            df = self.read_csv(dataset_name, training_data_filename)
            return df.columns.tolist()
        except Exception as e:
            print("An error occurred while getting headers:", e)
            raise e


    """
    
    Get the top 25 rows of the dataset as a CSV string

    """
    def get_top_25_rows(self, dataset_name, training_data_filename):
        try:
            df = self.read_csv(dataset_name, training_data_filename)

            return df.head(25).to_csv(index=False, header=False)
        except Exception as e:
            print("An error occurred while getting top 25 rows:", e)
            raise e


    """
    
    Read a CSV file from the dataset directory into a pandas DataFrame
    
    """
    def read_csv(self, dataset_name, training_data_filename):
        try:
            dataset_path = Path(self.workdir) / dataset_name
            csv_file = dataset_path / f"{training_data_filename}.csv"

            if not csv_file.exists():
                raise FileNotFoundError(f"Dataset file {csv_file} not found")

            df = pd.read_csv(csv_file, engine='python', quotechar='"', on_bad_lines='skip')
            return df
        except Exception as e:
            print("An error occurred while reading the CSV file:", e)
            raise e


    """
    
    Get the list of datasets in the directory
    
    """
    def list_datasets(self, dataset_path):
        try:
            csv_files = list(dataset_path.glob("*.csv"))
            if not csv_files:
                raise FileNotFoundError(f"No CSV files found in {dataset_path}")

            return csv_files
        except Exception as e:
            print("An error occurred while listing datasets:", e)
            raise e
