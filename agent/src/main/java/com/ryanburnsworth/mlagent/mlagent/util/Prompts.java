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
            """);

    public static final PromptTemplate DATA_PREPROCESSING_PROMPT = new PromptTemplate("""
                    You are generating one or more **Jupyter notebook cells** that will be **appended to an existing notebook.**
                    Do NOT generate a full `.ipynb` file. Only return valid **cell objects** as JSON.
            
                    ## Context:
                    - A dataset has already been loaded into variables `df` (training) and `test_df` (optional validation).
                    - Your task is to **preprocess** this dataset for machine learning.
            
                    ## Previous Agent Memory (use this to avoid repeating previous work):
                    {memory}
            
                    ## Preprocessing Steps to Implement:
                    1. Detect and handle missing values (drop or impute depending on percentage of nulls).
                    2. Encode categorical columns (use LabelEncoder for binary, OneHotEncoder or pandas.get_dummies for multi-category).
                    3. Normalize or scale numerical columns (StandardScaler or MinMaxScaler).
                    4. Split dataset into X_train, y_train, (and X_test, y_test if `test_df` exists).
                    5. Print summaries of:
                        - Preprocessed feature columns
                        - Data types after encoding
                        - Scaling method used
            
                    ## Output Format (Strict Requirement):
                    Return **only a JSON array** of notebook cells. Do NOT include any top-level "cells" key or notebook metadata. The root must be an array.
                     - Each cell object must contain:
                       - "cell_type": either "code" or "markdown"
                       - "metadata": an empty object
                       - "source": a list of strings, each string representing a line of code or markdown ending with "\\n"
                       - "outputs": an empty list
                       - "execution_count": null
            
                     Do not include literal square brackets `[` or `]` in the prompt. Describe the array and list structure in words as above. The LLM should generate the actual JSON array.
            
                    ## Rules:
                    - Do NOT wrap output in ```markdown or ```json.
                    - Do NOT include explanation outside of the JSON.
                    - Assume pandas, numpy, sklearn are already imported — if not, import them.
                    - If `test_df` is None, skip processing test set.
            
                    Return ONLY the JSON object described above as the final output. Do not return a bare list.
            """);

    public static final PromptTemplate MODEL_TRAINING_PROMPT = new PromptTemplate("""
                You are generating one or more **Jupyter notebook cells** that will be **appended to an existing notebook.**
                Do NOT generate a full `.ipynb` file. Only return valid **cell objects** as JSON.
            
                ## Context:
                - The dataset has already been preprocessed and split into the following variables:
                  - `X_train`, `y_train`
                  - `X_test`, `y_test` (optional, may not exist)
                - Your task is to **train one or more machine learning models** on this dataset.
            
                ## Previous Agent Memory (use this to avoid repeating previous work):
                {memory}
            
                ## Model Training Requirements:
                1. Choose and initialize at least one appropriate model (e.g., LogisticRegression, RandomForestClassifier, XGBoost, Neural Network depending on dataset size).
                2. Train the model using `X_train` and `y_train`.
                3. If test data exists, evaluate using accuracy, precision, recall, F1-score, and confusion matrix.
                4. Print out:
                    - Model type used
                    - Training completion confirmation
                    - Evaluation metrics
            
                ## Output Format (Strict Requirement):
                Return **only a JSON array** of notebook cells. Do NOT include any top-level "cells" key or notebook metadata. The root must be an array.
                 - Each cell object must contain:
                   - "cell_type": either "code" or "markdown"
                   - "metadata": an empty object
                   - "source": a list of strings, each string representing a line of code or markdown ending with "\\n"
                   - "outputs": an empty list
                   - "execution_count": null
            
                 Do not include literal square brackets `[` or `]` in the prompt. Describe the array and list structure in words as above. The LLM should generate the actual JSON array.
            
                ## Rules:
                - Do NOT wrap output in ```markdown or ```json.
                - Do NOT include explanation outside of the JSON.
                - Assume sklearn.metrics (accuracy_score, precision_score, recall_score, f1_score, confusion_matrix) is imported — if not, import it.
                - If `X_test` or `y_test` does not exist, evaluate using cross-validation.
            
                Return ONLY the JSON array of notebook cell objects as the final output. Do not return a bare list.
            """);

    public static final PromptTemplate MODEL_EVALUATION_PROMPT = new PromptTemplate("""
                You are generating one or more **Jupyter notebook cells** that will be **appended to an existing notebook.**
                Do NOT generate a full `.ipynb` file. Only return valid **cell objects** as JSON.
            
                ## Context:
                - A machine learning model has already been trained.
                - Evaluation metrics and predictions are available as:
                    - `y_true` (actual labels)
                    - `y_pred` (predicted labels)
                    - If available, `y_proba` (predicted probabilities for ROC curve)
                - Your task is to **visualize the model performance**.
            
                ## Previous Agent Memory (use this to avoid repeating previous work):
                {memory}
            
                ## Visualization Requirements:
                1. Generate and display a Confusion Matrix using matplotlib.
                2. If `y_proba` exists and is binary classification, plot:
                    - ROC Curve with AUC score.
                    - Precision-Recall Curve.
                3. If the model has attribute `feature_importances_`, display a sorted bar chart of feature importance.
                4. Include clear titles and axis labels.
            
                ## Output Format (Strict Requirement):
                Return **only a JSON array** of notebook cells. Do NOT include any top-level "cells" key or notebook metadata. The root must be an array.
                 - Each cell object must contain:
                   - "cell_type": either "code" or "markdown"
                   - "metadata": an empty object
                   - "source": a list of strings, each string representing a line of code or markdown ending with "\\n"
                   - "outputs": an empty list
                   - "execution_count": null
            
                 Do not include literal square brackets `[` or `]` in the prompt. Describe the array and list structure in words as above. The LLM should generate the actual JSON array.
            
                ## Rules:
                - Do NOT wrap output in ```markdown or ```json.
                - Do NOT include explanation outside of the JSON.
                - Assume matplotlib.pyplot is imported as `plt` — if not, import it.
                - If any required variable is missing, add a markdown cell noting what is missing instead of causing an error.
            
                Return ONLY the JSON array of notebook cell objects as the final output. Do not return a bare list.
            """);

    public static final PromptTemplate ERROR_HANDLING_PROMPT = new PromptTemplate("""
                There was an error in the code you provided. Fix the errors provided and output a valid output.
            
                You will be provided with:
            
                userPrompt: {userPrompt} - the user's prompt
                aiResponse: {aiResponse} - your code response to the user
                errorMessage: {errorMessage} - the generic error message
                errorDetails: {errorDetails} - the stack trace
            
                Requirements:
                    - Rewrite the fixed code in its entirety. Not just the fixes.
                    - Output must be in a valid ipynb code format. Either an entire ipynb notebook or cells to be appended within an existing notebook depending on the aiResponse given above.
                    - Do not add additional text or comments. Return only the fixed code
            """);
}
