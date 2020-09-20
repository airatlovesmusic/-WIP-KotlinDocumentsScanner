import cv2
import imutils

# get image
image = cv2.imread("testimg.jpg")
ratio = image.shape[0] / 500.0
orig = image.copy()
image = imutils.resize(image, height=500)

# image filtering
gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
gray = cv2.GaussianBlur(gray, (5, 5), 0)
edged = cv2.Canny(gray, 75, 200)

# find the contours of the image
cnts = cv2.findContours(edged.copy(), cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
# grab contours
if len(cnts) == 2: cnts = cnts[0]
elif len(cnts) == 3: cnts = cnts[1]
cnts = sorted(cnts, key=cv2.contourArea, reverse=True)[:5]
# loop over the contours
for c in cnts:
    # approximate the contour
    peri = cv2.arcLength(c, True)
    approx = cv2.approxPolyDP(c, 0.02 * peri, True)
    if len(approx) == 4:
        screenCnt = approx
        break

# show the edges of the document
cv2.drawContours(image, [screenCnt], -1, (0, 255, 0), 2)
cv2.imshow("Out", image)

# flush
cv2.waitKey(0)
cv2.destroyAllWindows()
