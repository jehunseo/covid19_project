# covid19_project
- 블루투스 RSSI에 기반한 근처 접촉 사용자 기록을 남기고,
이를 통해 확진 혹은 자가격리 판정 시 접촉자에게 알리는 방식으로 COVID19 방역지침을 준수하는 시스템 구현

# 구현 목표
- BLE/WIFI 기반 밀첩접촉 자동로깅 기능
- 감염의심자 발생시 로깅된 디바이스에 대한 푸시알림 전송(Google FCM / Apple APN)
- 실내외의 밀집도정보 수집 및 지도 API를 통한 그래픽화된 정보 열람 기능
- 카메라 및 Open CV 알고리즘을 이용한 장소 밀집도 분석/전송 임베디드 모듈 구현

# 현재 구현 상황
- 클라우드 서버 https://ajouycdcovid19.com/dbAdmin/index.php