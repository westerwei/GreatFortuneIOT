import numpy
import cv2
import os
import shutil
import sys

def testModuleCall():
    print 'Test Module Call'


def faceDetection(fileName, isCvtGray=True):
    print 'Start to detect human face~~'
    
    face_cascade = cv2.CascadeClassifier('/Users/Wester/Pictures/tmp/haarcascade_frontalface_default_other.xml')
    eye_cascade = cv2.CascadeClassifier('/Users/Wester/Pictures/tmp/haarcascade_eye.xml')
#     face_cascade = cv2.CascadeClassifier('/Software/opencv-3.0.0-beta/data/haarcascades/haarcascade_frontalface_default.xml')
#     eye_cascade = cv2.CascadeClassifier('/Software/opencv-3.0.0-beta/data/haarcascades/haarcascade_eye.xml')
#     img = cv2.imread('/Users/Wester/Pictures/tmp/20141117-lens-lisa-slide-U31N-superJumbo.jpg')
#     img = cv2.imread('/Users/Wester/Pictures/tmp/Gender-Data/Male/cache2335955.jpg')
    img = cv2.imread(fileName)
    imgDetect = img
#     ff2top.png
    if isCvtGray:
        imgDetect = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
#     gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    faces = face_cascade.detectMultiScale(imgDetect, 1.3, 5)
#     faces = face_cascade.detectMultiScale(
#                 gray,
#                 scaleFactor=1.3,
#                 minNeighbors=5,
#                 minSize=(30, 30),
#                 flags=cv2.cv.CV_HAAR_SCALE_IMAGE
#             )
    
    imgList = []
    for (x,y,w,h) in faces:
        crop_img = img[y:(y+h), x:(x+w)]
        crop_img = cv2.resize(crop_img, (120,120))
        imgList.append(crop_img)
#         cv2.rectangle(img,(x,y),(x+w,y+h),(255,0,0),2)
#         roi_gray = gray[y:y+h, x:x+w]
#         roi_color = img[y:y+h, x:x+w]
#         eyes = eye_cascade.detectMultiScale(roi_gray)
#         for (ex,ey,ew,eh) in eyes:
#             cv2.rectangle(roi_color,(ex,ey),(ex+ew,ey+eh),(0,255,0),2)
    
#     cv2.imshow('img',img)
#     cv2.waitKey(0)
#     cv2.destroyAllWindows()
    
    return imgList

def scanAndGenerateGenderTrainingSet(srcFolder, destFolder):
    for file in os.listdir(srcFolder):
        if file == '.DS_Store':
            continue 
        
        imgList = faceDetection(srcFolder + file)
        i = 0
        if len(imgList) <= 0:
            print 'Can not detect face image:'+file
            
        for img in imgList:
            destFile = destFolder + file
            if i != 0:
                destFile = destFolder + str(i) + file
            cv2.imwrite(destFile, img)
            i = i+1    
    
def trainGenderModel():
    maleDataSet = '/Users/Wester/Pictures/tmp/Gender-Data/Male-Train/'
    femaleDataSet = '/Users/Wester/Pictures/tmp/Gender-Data/Female-Train/'
    fisherFaceRecog = cv2.createFisherFaceRecognizer();
    allImgs, allSubjects = [], []
    [mImgs, mSubjects] = readDataSet(maleDataSet, 1)
    allImgs.extend(mImgs)
    allSubjects.extend(mSubjects)
    
    [fImgs, fSubjects] = readDataSet(femaleDataSet, 2)
    allImgs.extend(fImgs)
    allSubjects.extend(fSubjects)
    
    allSubjects = numpy.asarray(allSubjects, dtype=numpy.int32)
    
    fisherFaceRecog.train(numpy.asarray(allImgs), numpy.asarray(allSubjects))
    
    return fisherFaceRecog
    
    
def readDataSet(path, label):
    imgs, subjects = [], []
    for sampleFile in os.listdir(path):
        if sampleFile == '.DS_Store':
            continue
        
#         sampleImg = cv2.imread(os.path.join(path, sampleFile), cv2.IMREAD_GRAYSCALE)
#         sampleImg = cv2.imread(os.path.join(path, sampleFile))
        sampleFaces = faceDetection(os.path.join(path, sampleFile), True)
        if len(sampleFaces) <= 0:
            print 'Can not detect face image:'+sampleFile
            continue
#         imgs.append(sampleImg)
#         subjects.append(label)
        for face in sampleFaces:
            imgs.append(cv2.cvtColor(face, cv2.COLOR_BGR2GRAY))
            subjects.append(label)        
    
    return [imgs, subjects]

def loadModel(modelPath):
    model = cv2.createFisherFaceRecognizer()
    model.load(modelPath)
    return model

def predictOnePic(modelFile, testImg):
#     testImg = 'y-POLL-articleLarge.jpg'
    imgList = faceDetection(testImg)
    i = 0
    predictResult = []
    genderModel = loadModel(modelFile)
    for img in imgList:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        [predictLabel, confVal] = genderModel.predict(numpy.asarray(gray))
        gender = 'male'
        if predictLabel == 2:
            gender = 'female'
        
        predictResult.append([predictLabel, confVal])    
#         cv2.imwrite('/Users/Wester/Pictures/tmp/gentmp/' + str(i) + '_' + gender + '_' + testImg, img)
#         i = i+1            

def testOneImageDetect():
    testImg = 'y-POLL-articleLarge.jpg'
    imgList = faceDetection('/Users/Wester/Pictures/tmp/' + testImg)
    i = 0
    for img in imgList:
        cv2.imwrite('/Users/Wester/Pictures/tmp/gentmp/' + str(i) + '_' + testImg, img)
        i = i+1

def testOneImageDetectWithGenderPredict(modelFile, testImg):
#     testImg = 'y-POLL-articleLarge.jpg'
    imgList = faceDetection('/Users/Wester/Pictures/tmp/' + testImg)
    i = 0
    genderModel = loadModel(modelFile)
    for img in imgList:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        [predictLabel, confVal] = genderModel.predict(numpy.asarray(gray))
        gender = 'male'
        if predictLabel == 2:
            gender = 'female'
            
        cv2.imwrite('/Users/Wester/Pictures/tmp/gentmp/' + str(i) + '_' + gender + '_' + testImg, img)
        i = i+1            

def faceDetectWithVideoCapture():
    modelFile = '/Users/Wester/Pictures/tmp/Gender-Data/Model/GenderModel.xml'
    face_cascade = cv2.CascadeClassifier('/Users/Wester/Pictures/tmp/haarcascade_frontalface_default_Android.xml')
    video_capture = cv2.VideoCapture(0)
    genderModel = loadModel(modelFile)
    while True:
        ret, frame = video_capture.read()
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = face_cascade.detectMultiScale(
                    gray,
                    scaleFactor=1.1,
                    minNeighbors=5,
                    minSize=(30, 30),
                    flags=cv2.cv.CV_HAAR_SCALE_IMAGE
                )
        for (x, y, w, h) in faces:
            crop_img = gray[y:(y+h), x:(x+w)]
            crop_img = cv2.resize(crop_img, (120,120))
            [predictLabel, confVal] = genderModel.predict(numpy.asarray(crop_img))
            if predictLabel == 1:
                cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
            else:
                cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 0, 255), 2)
        
        cv2.imshow('frame', frame)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
        
    video_capture.release()
    cv2.destroyAllWindows()
    
if __name__ == '__main__':
#     testOneImageDetect()
#     scanAndGenerateGenderTrainingSet('/Users/Wester/Pictures/tmp/Gender-Data/Female/', 
#                                      '/Users/Wester/Pictures/tmp/Gender-Data/Female-Crop/')
#     print '----------------------'
#     scanAndGenerateGenderTrainingSet('/Users/Wester/Pictures/tmp/Gender-Data/Male/', 
#                                      '/Users/Wester/Pictures/tmp/Gender-Data/Male-Crop/')    


    modelFile = '/Users/Wester/Pictures/tmp/Gender-Data/Model/GenderModel.xml'
       
#     genderModel = trainGenderModel()
#     genderModel.save(modelFile)
#     
#     testFolder = '/Users/Wester/Pictures/tmp/Gender-Data/TestingSample/'
#     testPredict = '/Users/Wester/Pictures/tmp/Gender-Data/TestingSample/predict/'
#     
#     genderModel = loadModel(modelFile)
#           
#     for testFile in os.listdir(testFolder):
#         if testFile == '.DS_Store' or testFile == 'predict':
#             continue
#         testingFaces = faceDetection(os.path.join(testFolder, testFile), True)         
# #         testImg = cv2.imread(os.path.join(testFolder, testFile), cv2.IMREAD_GRAYSCALE)
# #         testImg = cv2.resize(testImg, (120,120))
#         if len(testingFaces) <= 0:
#             continue
#         testImg = cv2.cvtColor(testingFaces.pop(), cv2.COLOR_BGR2GRAY);
#         [predictLabel, confVal] = genderModel.predict(numpy.asarray(testImg))
#                  
#         print testFile + ' : ' + str(predictLabel)
#                
#         gender = 'male'
#         if predictLabel == 2:
#             gender = 'female'
#                
#         shutil.copy(os.path.join(testFolder, testFile), os.path.join(testPredict, gender + '_' + str(confVal) + '_' + testFile))
    
#     testOneImageDetectWithGenderPredict(modelFile, '2008_Health.JPG')
    faceDetectWithVideoCapture()        
    pass