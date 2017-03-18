Wifi的feature重设为以下几项： 
 
* homeWifiLevel  
* meanOfOtherWifi
* isHomeWifiConnected
* stdOfOtherWifi
* sumVarOfHomeWifi (后5秒均值-前8秒均值）
* sumVarOfOtherWifi (后5秒均值-前8秒均值）

获取16组trainData和部分testData, 利用svm_scale.java 在命令行模式下对trainData进行normalization, 得到train_scale.txt 数据和对应的scale_para.txt(用此参数对test和真实数据进行normalization),而后再得到test_scale.txt。  

利用scale后的traindata在Eclipse中运行svm-train.java, 得到model.txt文件，将model的内容拷贝进android studio中的model-scale.txt文件中，同时把scale-para.txt文件保存进res/raw文件中，用于对实时数据的scale  

Android中已经对svm的predict和scale文件进行了modify
当前performance为： 
 
* 在靠近wifi信号源采集满10秒后，走到门口基本就可以提醒  
* 若在房间中部采集10秒信号，那么要稍微走出门口一段距离会有提醒
* 如果在贴近门口的位置采集，如果信号明显不强，可能会直接提醒
* 如果在门外远离信号的地方采集10s，直接就会提醒