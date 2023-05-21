from joblib import dump, load
import pandas as pd
import numpy as np
from sklearn.ensemble import IsolationForest
import os
from os.path import join,dirname
def train():
    input=join(os.environ["HOME"], "training.csv")
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

    df_new=df_new[['duration','user_id','pressure_mean'	,'pressure_std'	,'pressure_max'	,'size_mean'	,'size_std'	,'size_max']]

    #dataset
    dataset_input=join(dirname(__file__), "data.csv")
    df_dataset = pd.read_csv(dataset_input)
    print('input dataset success')
    # get the index of null value
    df_dataset[df_dataset['pressure'].isnull().values==True].index.tolist()

    # remove the rows with null value
    df_dataset = df_dataset.dropna(axis=0, how='any')

    df_dataset['pressure_mean'] = df_dataset['pressure'].apply(lambda x: str_to_array(x).mean())
    df_dataset['pressure_std'] = df_dataset['pressure'].apply(lambda x: str_to_array(x).std())
    df_dataset['pressure_max'] = df_dataset['pressure'].apply(lambda x: str_to_array(x).max())

    df_dataset['size_mean'] = df_dataset['size'].apply(lambda x: str_to_array(x).mean())
    df_dataset['size_std'] = df_dataset['size'].apply(lambda x: str_to_array(x).std())
    df_dataset['size_max'] = df_dataset['size'].apply(lambda x: str_to_array(x).max())

    # df['velocity_mean'] = df['velocity'].apply(lambda x: str_to_array(x).mean())
    # df['velocity_std'] = df['velocity'].apply(lambda x: str_to_array(x).std())
    # df['velocity_max'] = df['velocity'].apply(lambda x: str_to_array(x).max())

    # drop the pressure column
    df_dataset = df_dataset.drop(['pressure'], axis=1)
    df_dataset = df_dataset.drop(['size'], axis=1)
    df_dataset = df_dataset.drop(['velocity'], axis=1)

    df_dataset=df_dataset[df_dataset['direction']==6]
    df_dataset=df_dataset.drop('direction',axis=1)

    # combine
    df_all=pd.concat([df_dataset,df_new],axis=0)
    df_all.reset_index(drop=True,inplace=True)
    # import standard scaler
    from sklearn.preprocessing import StandardScaler

    # create an instance of the standard scaler
    scaler = StandardScaler()

    df_all, df_norm = df_all[['duration', 'user_id']], df_all.drop(['duration', 'user_id'], axis=1)

    columns = df_norm.columns

    df_norm = scaler.fit_transform(df_norm)

    # convert the normalized features into a dataframe with the column names intact
    df_norm = pd.DataFrame(df_norm, columns=columns)

    # concatenate the normalized features and the user_id column
    df_all = pd.concat([df_all, df_norm], axis=1)

    # df_all = df_all.fillna(0)

    df_positive = df_all.loc[df_all['user_id'] == 100]
    df_negative = df_all.loc[df_all['user_id'] != 100]

    # drop the user_id column
    df_positive = df_positive.drop(['user_id'], axis=1)
    df_negative = df_negative.drop(['user_id'], axis=1)

    X_train = df_positive.sample(frac=0.85, random_state=0)
    y_train = pd.DataFrame(np.ones((len(X_train), 1)))

    X_positive = df_positive.drop(X_train.index)
    y_positive = pd.DataFrame(np.zeros((len(X_positive), 1)))

    # randomly select negative samples equal to the number of x_test
    X_negative = df_negative.sample(n=len(X_positive), random_state=0)
    y_negative = pd.DataFrame(np.ones((len(X_negative), 1)))

    # concatenate the positive and negative samples
    X_test = pd.concat([X_positive, X_negative])
    y_test = pd.concat([y_positive, y_negative])

    model_if = IsolationForest(contamination = 0.1)
    model_if.fit(X_train)
    y_pred = model_if.predict(X_test)

    #  save
    # 保存模型
    model_output=join(os.environ["HOME"], "model.joblib")
    dump(model_if, model_output)

    # 加载模型
    loaded_model = load(model_output)
    print('load model success')
