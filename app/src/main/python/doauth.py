from joblib import dump, load
import pandas as pd
import numpy as np
from sklearn.ensemble import IsolationForest
import os
from os.path import join,dirname
def doau(user_id):
    data = []
    with open(join(os.environ["HOME"], user_id+'authen.csv'), 'r') as f:
        lines = f.readlines()
        for line in lines:
            row = line.split(',')
            data.append(row)
    df = pd.DataFrame(data)
    def str_to_array_new(str_array):
        str_array = str_array.replace('[', '').replace(']', '')
        str_array = np.array(str_array.replace('\n', '').split(','))
        str_array = np.where(str_array == '', 0, str_array)
        str_array = str_array.astype(np.float64)
        return str_array


    df_new = df[[0, 1]].copy()
    df_new['pressure'] = df.iloc[:, 2::2].apply(lambda x: ','.join(x.dropna()), axis=1).str.slice(0, -3)
    df_new['size'] = df.iloc[:, 3::2].apply(lambda x: ','.join(x.dropna()), axis=1)
    df_new['pressure_mean'] = df_new['pressure'].apply(lambda x: str_to_array_new(x).mean())
    df_new['pressure_std'] = df_new['pressure'].apply(lambda x: str_to_array_new(x).std())
    df_new['pressure_max'] = df_new['pressure'].apply(lambda x: str_to_array_new(x).max())

    df_new['size_mean'] = df_new['size'].apply(lambda x: str_to_array_new(x).mean())
    df_new['size_std'] = df_new['size'].apply(lambda x: str_to_array_new(x).std())
    df_new['size_max'] = df_new['size'].apply(lambda x: str_to_array_new(x).max())
    df_new = df_new.drop(['pressure'], axis=1)
    df_new = df_new.drop(['size', 0], axis=1)
    df_new.rename(columns={1: "duration"},inplace=True)

    model_output=join(os.environ["HOME"], user_id+"model.joblib")
    loaded_model = load(model_output)

    res=loaded_model.predict(df_new)
    print(res)
    return res