import os
from os.path import join,dirname
import pandas as pd

def sayHello():
    print("Hello World")
    # filename = join(os.environ["HOME"], "test.csv")
    filename = join(dirname(__file__), "filename.txt")
    # /data/data/com.example.swipeauth/files/chaquopy/AssetFinder/app/filename.txt
    print(filename)

    df = pd.DataFrame({
        'A': [1, 2, 3, 4],
        'B': [5, 6, 7, 8],
        'C': [9, 10, 11, 12],
        'D': [13, 14, 15, 16]
    })
    df.to_csv(filename)