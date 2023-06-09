from joblib import dump, load
import pandas as pd
import numpy as np
from sklearn.ensemble import IsolationForest
import os
from os.path import join,dirname
def train(user_id):
    input=join(os.environ["HOME"], user_id+"training.csv")
    # read csv file
    # df = pd.read_csv('training.csv')
    data = []
    with open(input, 'r') as f:
        lines = f.readlines()
        for line in lines:
            row = line.split(',')
            data.append(row)

    df = pd.DataFrame(data)

    def str_to_array(str_array):
        str_array = str_array.replace('[', '').replace(']', '')
        str_array = np.array(str_array.replace('\n', '').split(' '))
        str_array = np.where(str_array == '', 0, str_array)
        str_array = str_array.astype(np.float64)
        return str_array
    def str_to_array_new(str_array):
        str_array = str_array.replace('[', '').replace(']', '')
        str_array = np.array(str_array.replace('\n', '').split(','))
        str_array = np.where(str_array == '', 0, str_array)
        str_array = str_array.astype(np.float64)
        return str_array

    df_new=df[[0,1]].copy()
    df_new['pressure']=df.iloc[:, 2::2].apply(lambda x: ','.join(x.dropna()), axis=1).str.slice(0, -3)
    df_new['size']=df.iloc[:, 3::2].apply(lambda x: ','.join(x.dropna()), axis=1)
    df_new['pressure_mean'] = df_new['pressure'].apply(lambda x: str_to_array_new(x).mean())
    df_new['pressure_std'] = df_new['pressure'].apply(lambda x: str_to_array_new(x).std())
    df_new['pressure_max'] = df_new['pressure'].apply(lambda x: str_to_array_new(x).max())

    df_new['size_mean'] = df_new['size'].apply(lambda x: str_to_array_new(x).mean())
    df_new['size_std'] = df_new['size'].apply(lambda x: str_to_array_new(x).std())
    df_new['size_max'] = df_new['size'].apply(lambda x: str_to_array_new(x).max())
    df_new=df_new.drop(['pressure'],axis=1)
    df_new=df_new.drop(['size',0],axis=1)

    df_new=df_new.iloc[0:44]
    # define user_id
    df_new['user_id']=100
    df_new.rename(columns={1: "duration"},inplace=True)

    df_new=df_new[['duration','pressure_mean'	,'pressure_std'	,'pressure_max'	,'size_mean'	,'size_std'	,'size_max']]



    model_if = IsolationForest(contamination = 0.1)
    model_if.fit(df_new)
    # y_pred = model_if.predict(X_test)

    #  save
    # 保存模型
    model_output=join(os.environ["HOME"], user_id+"model.joblib")
    dump(model_if, model_output)

    # 加载模型
    loaded_model = load(model_output)
    print('load model success')
