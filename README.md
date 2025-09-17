# Fix My Car - Vertex AI Search

У цьому посібнику описано, як розгорнути додаток **Fix My Car** в **GKE**, використовуючи **Vertex AI Search** як сховище даних.

<img width="2098" alt="arch-vertex-ai-search" src="https://github.com/RebelsBoss/Fix_my_car/assets/126337643/ba691211-6e73-42cb-90c9-99dd9f5a5a36">

## Передумови

Щоб розгорнути цей додаток, вам знадобиться:

- Проект [Google Cloud](https://cloud.google.com/resource-manager/docs/creating-managing-projects#creating_a_project) з увімкненим білінгом.
- [Google Cloud SDK (gcloud)](https://cloud.google.com/sdk/docs/install), встановлений і налаштований у вашому локальному середовищі (або використовуйте [Google Cloud Shell](https://cloud.google.com/sdk/docs/interactive-gcloud)).
- [Docker Engine](https://docs.docker.com/engine/install/), АБО інструмент з відкритим вихідним кодом, такий як [Colima](https://github.com/abiosoft/colima), який може виконувати **docker build** та **docker push**.
- Java 18+, Maven 3.9.6+
- Python 3.9+

## Створіть Artifact Registry repository

Тут ви зберігатимете зображення контейнерів для фронту **Streamlit** та бекенду **Java**. [Контейнери](https://cloud.google.com/learn/what-are-containers) - це зібраний вихідний код і залежності, які можна розгортати в різних середовищах.

1. Відкрийте консоль **Google Cloud**. Відкрийте пошуковий рядок і введіть **"Artifact Registry"**. Відкрийте сторінку консолі **Artifact Registry**.
2. Натисніть **Create Repository**.
3. Назвіть ваш репозиторій **fixmycar**. Залиште **Docker** по дефолту. Виберіть будь-який регіон, наприклад **us-central1**. Натисніть кнопку **Create**.

## Build та push зображення контейнерів.

1. У консолі **Artifact Registry** виберіть ваш репозиторій, **fixmycar**. Потім натисніть **Setup Instructions**.
2. Скопіюйте команду автентифікації, наприклад

```
gcloud auth configure-docker us-central1-docker.pkg.dev
```

3. Відкрийте термінал або **Cloud Shell**. Вставте команду і запустіть її. Це дозволить автентифікувати ваш **Docker**-клієнт у вашому **Artifact Registry**.

**Очікуваний результат:**

```
{
  "credHelpers": {
    "us-central1-docker.pkg.dev": "gcloud"
  }
}
Adding credentials for: us-central1-docker.pkg.dev
```

4. Відкрийте скрипт **dockerpush.sh** у корені цього каталогу. Замініть **PROJECT_ID** на ідентифікатор вашого проекту в **Google Cloud**.
5. Запустіть скрипт, щоб створити і перенести зображення контейнерів **Frontend** і **Backend** до **Artifact Registry**.

```
cd GCP_Fix_my_car/
sudo chmod -R 777 *
./dockerpush.sh
```

**Очікуваний результат:**

```
latest: digest: sha256:864589160d7c3f982472427ed008cc03cf244f5db61a0c7312caaaa670ee0e47 size: 1786
✅ Container build and push complete.
```

## Створіть GKE Autopilot cluster

Ви розгорнете ці два образи контейнерів (**"frontend"** і **"backend"**) на [Google Kubernetes Engine (GKE)](https://cloud.google.com/kubernetes-engine/docs/concepts/kubernetes-engine-overview#how_works). **GKE** подбає про запуск цих двох серверів на базових обчислювальних ресурсах.

1. Відкрийте консоль **Google Cloud**. Відкрийте пошуковий рядок і введіть **"Kubernetes Engine"**. Відкрийте сторінку консолі **Kubernetes Engine**. (Якщо вам буде запропоновано увімкнути **API**, натисніть **"Enable"**).
2. У консолі **Kubernetes Engine** натисніть **Create**, щоб відкрити майстер створення кластера.
3. Збережіть всі налаштування за замовчуванням **(GKE Autopilot)**. Дайте кластеру будь-яку назву, наприклад, **fixmycar**. Натисніть кнопку **Create**.

**Це займе кілька хвилин.**

4. Коли кластер буде готовий, клацніть на назві кластера і натисніть **Connect**. Скопіюйте команду **"Command line Access"**, наприклад

```
gcloud container clusters get-credentials fixmycar --region us-central1 --project my-project
```

5. Поверніться до свого терміналу і вставте цю команду, а потім запустіть її.

**Очікуваний результат:**

```
Fetching cluster endpoint and auth data.
kubeconfig entry generated for fixmycar.
```

6. Перевірте, чи можете ви відповідає ваш кластер **GKE**:

```
kubectl cluster-info
```

Ви повинні побачити щось на кшталт:

```
Kubernetes control plane is running at https://34.69.121.152
...
```

## Завантажуйте інструкції до Cloud Storage

1. Відкрийте **Cloud Console** і знайдіть **"Cloud Storage"**. Перейдіть на сторінку консолі.
2. Натисніть кнопку **"Create Bucket"**.
3. Назвіть свій бакет чимось глобально унікальним, наприклад, **"your-project-id"-fixmycar**. Залиште всі інші налаштування за замовчуванням і натисніть кнопку **Create**. (Можливо, ви побачите сповіщення про ввімкнення заборони публічного доступу - це очікувано. Натисніть **"Confirm"**).
4. Скопіюйте повну назву вашого бакету, натиснувши на іконку **"copy"**.

<img width="868" alt="bucket-uri-copy" src="https://github.com/RebelsBoss/Fix_my_car/assets/126337643/e38d3bcc-9fe8-4a0d-b474-1743ce323b0d">

5. Відкрийте термінал. Задайте ім'я вашого бакету як змінну оточення.

```
export BUCKET_NAME=<your-bucket-name>
```

6. Завантажте інструкцію **Cymbal Starlight 2024** з публічного сховища.

```
gsutil cp gs://github-repo/generative-ai/sample-apps/fixmycar/cymbal-starlight-2024.pdf .
```

7. Завантажте посібник користувача **Cymbal Starlight 2024** до свого особистого хмарного сховища.

```
gsutil -m cp -r cymbal-starlight-2024.pdf gs://$BUCKET_NAME
```

**Очікуваний результат:**

```
- [10/10 files][156.6 MiB/156.6 MiB] 100% Done
Operation completed over 10 objects/156.6 MiB.
```

## Налаштуйте Vertex AI Search

1. Відкрийте **Cloud Console** і знайдіть **"Search and Conversation"**. Відкрийте сторінку консолі. Якщо з'явиться відповідний запит, натисніть **"Activate API"**.
2. Натисніть **Create a new app**, а потім виберіть **Search**.
3. Збережіть **"Enterprise edition features"** та **"Advanced LLM features"**.
4. Назвіть ваш додаток **[PROJECT_ID]-fixmycar** і встановіть відповідний ідентифікатор. Встановіть **"Company Name"** на **fixmycar**. Залиште регіон **global**.
5. Натисніть кнопку **Continue**. Потім натисніть **Create a new data store**.
6. У розділі **"Select Data Source"** натисніть **"Cloud Storage"**. Потім перейдіть до **manuals/** каталогу вашого бакету у **Cloud Storage**. Натисніть кнопку **Continue**.
7. Встановіть регіон **global**, а тип **Default Document Parser** - [**OCR Parser**](https://cloud.google.com/generative-ai-app-builder/docs/parse-chunk-documents#parser-types). Назвіть ваше сховище даних **fixmycar**.

<img width="1805" alt="datastore-creation2" src="https://github.com/RebelsBoss/Fix_my_car/assets/126337643/b36fe869-5f02-4c05-8d50-a9a50ee04ec3">

8. Натисніть **Create**, щоб створити сховище даних.
9. Повернувшись до майстра створення програми, виберіть сховище даних, яке ви щойно створили, а потім натисніть **Create**, щоб створити вашу програму.

## Тестуємо Vertex AI Search сховище даних

1. Повернувшись на сторінку **Data Stores**, виберіть нове сховище даних. Потім перейдіть на вкладку **Activity**. Тут ви можете переглянути стан обробки **PDF**-файлів. В основі **Vertex AI Search** лежить сканування документів і перетворення їх у векторні вбудовування. Потім він зберігає ці вбудовування в сховищі даних. **Це може зайняти близько 10 хвилин.**

<img width="1813" alt="datastore-activity" src="https://github.com/RebelsBoss/Fix_my_car/assets/126337643/bed6298e-b2d0-4ae9-88c1-2afbc806ffb4">

2. Коли ваше сховище даних буде готове, ви побачите зелену галочку і статус: **"Import completed"**.

<img width="1354" alt="vais-ready" src="https://github.com/RebelsBoss/Fix_my_car/assets/126337643/b8c3cf98-cd05-4b53-9562-47d0e84bd814">

3. Ви можете перевірити пошуковий запит безпосередньо з консолі, натиснувши **Apps** на лівій бічній панелі, а потім **Preview**. Наприклад, введіть запит: **Cymbal Starlight 2024: Максимальна вантажопідйомність**. Повинен з'явитися згенерований результат.

<img width="1369" alt="vais-preview-ui" src="https://github.com/RebelsBoss/Fix_my_car/assets/126337643/95bc01f0-5028-4b62-94ae-052fa874e992">

## Налаштуйте авторизацію GKE на Vertex AI

Ваш сервер бекенда на базі **GKE** повинен мати доступ до **Vertex AI Search** і **Gemini API Vertex AI**. Для цього ми зіставимо обліковий запис служби **Kubernetes**, який використовується бекенд-подом, з обліковим записом служби **Google Cloud IAM** з відповідними дозволами. Тут прив'язка облікового запису служби **Kubernetes** до служби **GCP** забезпечує автентифікацію ("хто ви?"), а ролі **IAM** облікового запису служби **GCP** забезпечують авторизацію ("що вам дозволено робити?"). Таке налаштування називається [**GKE Workload Identity**](https://cloud.google.com/kubernetes-engine/docs/concepts/workload-identity).

Також необхідно додати **PROJECT-ID** до скрипту **workload_identity.sh**.

Запустіть скрипт встановлення, щоб налаштувати авторизацію.

```
sudo chmod 777 workload_identity.sh
./workload_identity.sh
```

**Очікуваний результат:**

```
✅ Workload Identity setup complete.
```

## Деплоїмо додаток до GKE

1. Підготуйтеся до розгортання фронтенду та бекенду на Kubernetes Engine **(GKE)**, відкривши файл **kubernetes/frontend-deployment.yaml**.
2. Оновіть поле **image**, щоб використовувати, наприклад, ідентифікатор вашого проекту:

```
image: us-central1-docker.pkg.dev/project123/fixmycar/frontend:latest
```

3. Повторіть крок 2 для **kubernetes/backend-deployment.yaml**.
4. Оновіть **env var GCP_PROJECT_ID** у файлі **backend-deployment.yaml**, щоб використовувати ідентифікатор вашого проекту.


```
- name: GCP_PROJECT_ID
  value: "your-project-id"
```

5. Oновіть **env var VERTEX_AI_DATASTORE_ID** у файлі **backend-deployment.yaml**, щоб використовувати ваш **Vertex AI Search Data Store ID**. Ви можете знайти його в консолі, натиснувши **"Data"** і скопіювавши значення **Data Store ID**.

```
- name: VERTEX_AI_DATASTORE_ID
  value: "your-datastore-id"
```

6. Розгорніть програму на кластері **GKE**. Це створить розгортання (**"Pods"**, або запущені сервери) як для інтерфейсу **Streamlit**, так і для бекенда **Java**. Також будуть створені служби, які відкриють обидва сервери для публічного доступу до Інтернету.

```
kubectl apply -f kubernetes/backend-deployment.yaml
kubectl apply -f kubernetes/backend-service.yaml
kubectl apply -f kubernetes/frontend-deployment.yaml
kubectl apply -f kubernetes/frontend-service.yaml
```
Також **бажано** додати у деплой наступну змінну оточення:
```
kubectl set env deployment/fixmycar-backend GOOGLE_CLOUD_PROJECT="your_project_id"
```
Для видалення:

```
kubectl delete -f kubernetes/backend-deployment.yaml
kubectl delete -f kubernetes/backend-service.yaml
kubectl delete -f kubernetes/frontend-deployment.yaml
kubectl delete -f kubernetes/frontend-service.yaml
```

Ви можете побачити попередження про коригування ресурсів **Autopilot**. Це нормально, [**GKE Autopilot**](https://cloud.google.com/kubernetes-engine/docs/concepts/autopilot-resource-requests#overview) масштабує свої обчислювальні ресурси для виконання ваших робочих навантажень.

```
Warning: autopilot-default-resources-mutator:Autopilot updated Deployment default/fixmycar-frontend: adjusted resources to meet requirements for containers [fixmycar-frontend] (see http://g.co/gke/autopilot-resources)
```

## Тестуємо функціонал

1. Отримайте інформацію про стан ваших запущених **GKE**-подів, щоб переконатися, що фронт і бек запустилися успішно.

```
kubectl get pods
```

Примітка: якщо ви розгортаєте кластер уперше, може знадобитися ~3 хвилини для переходу від стану **Pending** до стану **Running**. (**GKE** масштабує ваш кластер)

**Очікуваний результат:**

```
NAME                                 READY   STATUS    RESTARTS      AGE
fixmycar-backend-74fbb8c8b5-zbcmv    1/1     Running   0             3m
fixmycar-frontend-75ff59c776-h57r6   1/1     Running   0             2m
```

2. Скопіюйте external IP вашого фронтенд-сервісу.

```
kubectl get service fixmycar-frontend
```

**Очікуваний результат:**

```
➜ kubectl get service fixmycar-frontend
NAME                TYPE           CLUSTER-IP       EXTERNAL-IP     PORT(S)        AGE
fixmycar-frontend   LoadBalancer   <ip-value>       <ip-value>      80:30519/TCP   4m13s
```

3. Відкрийте цю **IP**-адресу у веб-браузері. Ви побачите інтерфейс **Streamlit** з вікном чату.
4. Протестуйте підказку чату на основі існуючого пункту в посібнику користувача **Cymbal Starlight**.

Наприклад, ви можете перепитати про максимальну вантажопідйомність автомобіля. Цей текст знаходиться в нашому сховищі даних **Vertex**, тому наш додаток зможе знайти цю інформацію для нас.

<img width="744" alt="cargo-manual" src="https://github.com/RebelsBoss/Fix_my_car/assets/126337643/46df986d-9cc6-428e-a3f8-a6b00c9d80c2">

Запропонуйте додаток **Streamlit**:

```
Cymbal Starlight 2024: What is the max cargo capacity?
```

**Очікуваний результат:**

<img width="1338" alt="cargo-capacity-streamlit" src="https://github.com/RebelsBoss/Fix_my_car/assets/126337643/950ab501-24f3-44bb-a510-831465805b10">

Відповідь має відповідати тексту з інструкції.
Ви можете побачити, що відбувалося **"під капотом"**, переглянувши логи бекенд-сервера:

```
kubectl logs -l app=fixmycar-backend
```

**Очікуваний результат:**

```
fixmycar-backend-77cb969894-cfvbq fixmycar-backend 2024-03-23T23:35:07.059Z  INFO 1 --- [nio-8080-exec-4] c.c.f.FixMyCarBackendController          : 🔍 Vertex AI Search results: Chapter 6: Towing, Cargo, and Luggage Towing Your Cymbal Starlight 2024 is not equipped to tow a trailer. Cargo The Cymbal Starlight 2024 has a cargo capacity of <b>13.5 cubic feet</b>. The cargo area is located in the trunk of the vehicle.
fixmycar-backend-77cb969894-cfvbq fixmycar-backend 2024-03-23T23:35:07.060Z  INFO 1 --- [nio-8080-exec-4] c.c.f.FixMyCarBackendController          : 🔮 Gemini Prompt: You are a helpful car manual chatbot. Answer the car owner's question about their car. Human prompt: Cymbal Starlight 2024: what is the max cargo capacity?,
fixmycar-backend-77cb969894-cfvbq fixmycar-backend  Use the following grounding data as context. This came from the relevant vehicle owner's manual: Chapter 6: Towing, Cargo, and Luggage Towing Your Cymbal Starlight 2024 is not equipped to tow a trailer. Cargo The Cymbal Starlight 2024 has a cargo capacity of <b>13.5 cubic feet</b>. The cargo area is located in the trunk of the vehicle.
fixmycar-backend-77cb969894-cfvbq fixmycar-backend 2024-03-23T23:35:07.762Z  INFO 1 --- [nio-8080-exec-4] c.c.f.FixMyCarBackendController          : 🔮 Gemini Response: The Cymbal Starlight 2024 has a cargo capacity of 13.5 cubic feet. The cargo area is located in the trunk of the vehicle.
```

Тут ви бачите, як бекенд виконує пошуковий запит до **Vertex AI Search**, а потім доповнює підказку **Gemini**, використовуючи результати. Потім відповідь **Gemini** надсилається назад до фронтенду, і саме її ви бачите як відповідь чат-бота у браузері.
