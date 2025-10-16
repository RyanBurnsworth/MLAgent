package com.ryanburnsworth.mlagent.mlagent.util;

import org.springframework.ai.chat.prompt.PromptTemplate;

public class Prompts {
    public static final PromptTemplate DATA_LOADING_PROMPT = new PromptTemplate("""
                     Create a Jupyter notebook cell that loads and inspects a dataset. Your task:
            
                    You are provided with the following dataset metadata:
            
                    Title: {title}
                    Subtitle: {subtitle}
                    Description: {description}
                    Dataset files: {datasets}
            
                    1. Load the training and testing datasets from the Dataset files in the dataset metadata. 
                        If only one dataset, use only training. Use the exact filenames given in Dataset files.
                    2. Display the first few rows of the training set using head()
                    3. Show the training dataset info using info()
                    4. Provide statistical summary on the training set using describe()
                    5. Print the training dataset shape and column names
                    6. Identify the target variable (look for common names like 'target', 'label', 'class', 'y', or the last column)
                    7. Include Markdown cells:
                    8. At the top: introduce the dataset and the problem to solve.
                    9. Detect if a GPU is available (using PyTorch or TensorFlow) and configure the environment to use the GPU. 
                        If no GPU is available, default to CPU. Include the detected device in the dataset summary.
            
                    Requirements:
                    - Import all necessary libraries (pandas, numpy, etc.)
                    - Handle common file formats (CSV, JSON, Excel)
                    - Include error handling for file loading
                    - Print key information clearly:
                      - Dataset shape
                      - Column names and data types
                      - Missing value counts per column
                      - Suspected target variable name
                      - Sample of the data
                      - Output must be in a valid ipynb format.
                      - Do not add additional text or wrap the code in ```markdown or json ```
                    Store the loaded training dataset in a variable called 'df' for the next step.
                    Store the loaded validation dataset, if any, in a variable called 'test_df' for the next step.
                    GPU must be used automatically if available for any future ML tasks.
            
                    dataset_summary = \\{
                        "shape": dataset shape,
                        "columns": number of columns,
                        "missing_values": are there missing values,
                        "sample_data": dataset head,
                        "statistical_summary": dataset description,
                        "info": dataset info
                    \\}
                    ONLY RETURN THIS JSON OBJECT as the final output. Do not return empty notebook cells, extra code, or unrelated comments.
            """);

    public static final String GENERATE_NOTEBOOK_PROMPT = """
            You are an AI assistant tasked with generating a complete Jupyter notebook that trains a machine learning model on a given dataset.
            
            You are provided with the following dataset metadata:
            
            Title: {record["title"]}
            Subtitle: {record["subtitle"]}
            Description: {record["description"]}
            Columns: {record["headers"]}
            Sample data: {record["top25"]}
            Dataset files: {record["datasets"]}
            
            Notebook Requirements:
            
            Include Markdown cells:
            
            At the top: introduce the dataset and the problem to solve.
            
            Before each code block: explain what it does and why.
            
            At the end: summarize the results and insights.
            
            Include Code cells to:
            
            Load the dataset(s) and inspect it (head(), info(), describe()).
            
            Perform preprocessing (handle missing values, encode categorical variables, normalize/scale as needed).
            
            Split data into train/test sets.
            
            Train an appropriate ML model (regression, classification, clustering, etc.) based on the dataset.
            
            Evaluate the model using relevant metrics.
            
            The last output cell must produce a JSON object containing the following:
            
            {
              "accuracy": ...,
              "precision": ...,
              "recall": ...,
              "f1_score": ...,
              "model_used": "<model_used>"
            }
            
            <model_used> should indicate which model was trained (e.g., RandomForestClassifier, LogisticRegression, etc.).
            
            Include comments in code explaining key steps.
            
            Use popular Python libraries (pandas, numpy, matplotlib/seaborn, scikit-learn).
            
            Task: Using the dataset metadata, generate a Jupyter notebook that trains a machine learning model, evaluates it, and outputs a JSON object with accuracy, precision, recall, F1 score, and the model used as the last output. Include code, Markdown explanations, visualizations, and all intermediate outputs.
            """;

    public static final String EVALUATOR_PROMPT = """
            You are an AI assistant tasked with evaluating a machine learning model's performance based on its metrics.
            
            You will receive a JSON object with the following structure:
            
            {
              "accuracy": ...,
              "precision": ...,
              "recall": ...,
              "f1_score": ...,
              "model_used": "<model_used>"
            }
            
            Instructions:
            
            Analyze the metrics and determine if this model performs well enough. Consider that higher values are better for all metrics.
            
            If the performance is acceptable, your output should be:
            
            { "decision": "keep", "reason": "<brief reasoning>" }
            
            If the performance is insufficient, your output should be:
            
            { "decision": "try_again", "reason": "<brief reasoning>", "suggested_model": "<optional alternative model>" }
            
            Base your reasoning on general ML guidelines:
            
            High F1 score is important if the dataset is imbalanced.
            
            Accuracy alone is not sufficient for imbalanced datasets.
            
            Consider precision vs recall trade-offs depending on the task.
            
            Keep your JSON output strictly in the specified format—do not add extra text.
            
            Task: Evaluate the provided metrics and decide whether to keep this model or try a different one. Include a brief reason and optionally suggest an alternative model if needed.
            """;
}
