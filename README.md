# 🎬 Video Editor API

  

A **Video Editor API** da **L8Group** é desenvolvida com **Java 17** e **Spring Boot 3**, a API integra o **FFmpeg** para realizar operações avançadas, como **corte**, **redimensionamento**, **inserção de sobreposição de texto** (watermark) e **conversão de formatos**. Para garantir alto desempenho e escalabilidade, o processamento é realizado de forma **assíncrona** por meio do **RabbitMQ**, permitindo a manipulação eficiente de milhares de vídeos por dia. Os arquivos processados são armazenados com segurança na **AWS S3**, otimizando o gerenciamento e a distribuição de conteúdo. A API também incorpora autenticação e autorização por meio de **JWT (JSON Web Token)**, assegurando o controle de acesso às funcionalidades. Além disso, o sistema conta com **monitoramento detalhado** e métricas expostas via **Prometheus**, facilitando a observabilidade, a detecção de falhas e a manutenção preventiva.

## 📦 Tecnologias e Bibliotecas Principais

 
- 🌱 Spring Boot 3.4.1
- ☕ Java 17
- 🎞️ FFmpeg
- 🐰 RabbitMQ
- ☁️ AWS SDK v2 – S3
- 🐘 PostgreSQL
- 🔐 JWT 
- 📊 Prometheus
- ✨ Lombok 
---
## 🚀 Funcionalidades Principais

- ✅ Upload de vídeos
- ✂️ Corte de trechos
- 🖼️ Adição de sobreposição de texto
- 📐 Redimensionamento
- 🔁 Conversão de formatos
- ☁️ Upload para AWS S3
- 📊 Monitoramento com Prometheus
- 🕓 Processamento assíncrono com RabbitMQ

---


# 👤 Configuração de Usuário IAM e Credenciais AWS para o Projeto `video-editor-api`

Criar e configurar um usuário IAM com permissões adequadas para acesso ao Amazon S3, utilizado no projeto `video-editor-api`.

----------

## ✅ 1. Criar Usuário IAM

### 🔧 Etapas:

1.  Acesse o console de gerenciamento IAM da AWS:  
    👉 [https://console.aws.amazon.com/iam/](https://console.aws.amazon.com/iam/)
    
2.  Navegue até o menu **Users** e clique em **Create user**.
    
3.  Preencha as informações básicas:
    
    -   **User name**: `seu-nome-personalizado` (ex.: `calos.souza`)
        
4.  Clique em **Next** para avançar à etapa de permissões.
    

----------

## 🔐 2. Conceder Permissões

### 📌 Etapas:

1.  Em **Set permissions**, selecione a opção **Attach policies directly**.
    
2.  Marque a política necessária:
    
    -   `AmazonS3FullAccess` ✅ _(acesso completo ao Amazon S3)_

3.  Clique em **Next**, revise os detalhes e finalize com **Create user**.
    

----------


## 🔑 3. Gerar e Armazenar Access Keys

### Etapas:

1.  Após criar o usuário, clique sobre o nome do usuário criado.
    
2.  Vá até a aba **Security credentials**.
    
3.  Na seção **Access keys**, clique em **Create access key**.
    
4.  Na tela seguinte:
    
    -   Selecione a opção **Other**
        
    -   Clique em **Next**
        
5.  Na tela de revisão, clique em **Create access key**.
    
6.  Copie e armazene com segurança as credenciais geradas:
    

```ini
Access Key ID:       ID da chave de acesso
Secret Access Key:   Chave de acesso secreta
```
> ⚠️ **Atenção:** A **Secret Access Key será exibida apenas uma vez**. Armazene-a com segurança. Caso perca, será necessário excluir e criar uma nova chave.
---

## 📂 Configuração Local de Credenciais AWS


Para que o projeto `video-editor-api` possa autenticar-se corretamente com os serviços da AWS (como o Amazon S3), é necessário configurar os arquivos `config` e `credentials` do AWS CLI em um diretório específico da aplicação.

----------

### 🧾 Arquivo: `config`

-   **Caminho do arquivo:**
    
```ini
<raiz-do-projeto>/video-editor-api/.docker_aws/config 
```
-   **Conteúdo do arquivo:**
   

```ini
[profile editor-video-s3] 

region = us-east-1 
output = json 
```

----------

### 🔑 Arquivo: `credentials`

-   **Caminho do arquivo:**

```ini
<raiz-do-projeto>/video-editor-api/.docker_aws/credentials
```
-   **Conteúdo do arquivo:**
```ini
[editor-video-s3]

aws_access_key_id = SUA_ACCESS_KEY_ID 
aws_secret_access_key = SUA_SECRET_ACCESS_KEY 
```

---



# 📁 Configuração do Bucket S3: `api-editor-video`

Esta seção descreve como criar e configurar corretamente o bucket S3 `api-editor-video`, utilizado no projeto `video-editor-api` para armazenar vídeos brutos e processados.


## 🪣 1. Criação do Bucket S3

### 🔧 Etapas:

1.  Acesse o [AWS S3 Console](https://console.aws.amazon.com/s3/).
    
2.  Clique em **Create bucket**.
    
3.  Preencha os campos:
    
    -   **Bucket name**:
        ```ini
		    api-editor-video
         ```
    -   **AWS Region**:
		   ```ini
	        US East  (N. Virginia) - us-east-1
		   ```
   
4.  Em **Object Ownership**:
    
    -   Mantenha selecionado: `ACLs disabled (recommended)`
        
    -   Escolha: ✅ `Bucket owner enforced`
        
5.  Em **Block Public Access settings for this bucket**:
    
    -    **Mantenha todas as opções marcadas**

6.  Clique em **Create bucket**.

---

  

## 🔐 2. Configuração da Bucket Policy (Política de Acesso)

  A política define as permissões de acesso a objetos no bucket. A seguir, um exemplo de política com permissões de upload e leitura em todo o bucket.


## 📌 Etapas para aplicar a política:

1.  No console do S3, acesse o bucket `api-editor-video`.
    
2.  Vá até a aba **Permissions**.
    
3.  Na seção **Bucket policy**, clique em **Edit**.
    
4.  Cole e salve o seguinte JSON:

```json
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Sid": "AllowAppAccess",
			"Effect": "Allow",
			"Principal": {
				"AWS": "arn:aws:iam::123456789012:user/seu-usuario"
			},
			"Action": [
				"s3:GetObject",
				"s3:PutObject"
			],
			"Resource": "arn:aws:s3:::api-editor-video/*"
		}
	]
}
```

### 💡Substitua `123456789012` pelo seu ID da conta AWS e `seu-usuario` pelo nome exato do seu IAM User.

---


# 📥 Clonando o Repositório

  
```bash
git clone https://github.com/MichaelHCSilva/video-editor-api.git
```
---
## ⚙️ Configuração do Prometheus para monitorar a API de edição de vídeos

No arquivo `prometheus.yml`, configure o IP e porta do serviço da API que será monitorado. Exemplo:

```yaml
scrape_configs:
  - job_name: 'video-editor-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['SEU_IP_AQUI:8080']
```
---




# 🐳  Inicialização com Docker Compose

A aplicação é composta por múltiplos serviços (API de edição de vídeos, RabbitMQ, Prometheus etc.). Utilize o seguinte comando para **construir** e **iniciar todos os containers**:

A aplicação é composta por múltiplos serviços, incluindo:

- API de edição de vídeos
- RabbitMQ
- Prometheus

### 🔧 Para construir e iniciar todos os containers:
```bash
docker compose up --build
```


# 🚀Serviços Auxiliares Disponíveis

Após a inicialização via Docker, os seguintes serviços estarão disponíveis localmente:

---

#### 🐰 RabbitMQ – _Gerenciador de Filas_

- **URL:** [http://localhost:15672](http://localhost:15672)
- **Usuário:** `guest`
- **Senha:** `guest`

> Interface para monitoramento e gerenciamento das filas de mensagens utilizadas na orquestração das operações de vídeo (ex: `video.upload`, `video.cut`, `video.resize` etc.).
---

#### 📊 Prometheus – _Monitoramento e Métricas_

- **URL:** [http://localhost:9090](http://localhost:9090)

> Utilizado para consultar e visualizar métricas de desempenho e estado dos serviços da aplicação em tempo real.

## 🎯 Exemplos de Consultas no Prometheus

 - **🔍 Todas as métricas relacionadas a vídeos:**

		{__name__=~".*video_.*"}

-  **👤 Métricas do serviço de usuários:**
	
		{__name__=~".*user_.*"}

- **📦 Métricas de validação e armazenamento de arquivos:**

		{__name__=~"video_file_.*"}

- **⏱️ Upload de vídeos:**

		{__name__=~"video_upload_.*"}

- **✂️ Corte de vídeo:**

	   {__name__=~"video_cut_.*"}	

- **📐 Redimensionamento de vídeo:**
			
		{__name__=~"video_resize_.*"}

- **🖊️ Aplicação de overlay (marca d'água)**

		{__name__=~"video_overlay_.*"}
		
- **🔄 Conversão de formatos**
	
		{__name__=~"video_conversion_.*"}

- **⬇️ Serviço de download:**
	
		{__name__=~"video_download_.*"}

- **⚙️ Processamento em lote:**
			
		{__name__=~"video_batch_.*"}


---

# 🔐 Autenticação com (JWT)

Esta API utiliza **JSON Web Tokens (JWT)** para autenticação e autorização de usuários. Para acessar endpoints protegidos, é necessário registrar-se, realizar o login e incluir o token JWT nas requisições via cabeçalho `Authorization`.
  

# 📝 Registro de Usuário

  
> Cria uma nova conta de usuário no sistema.

-   **Endpoint**: `POST /auth/register`
    
-   **URL completa**: `http://localhost:8080/auth/register`

### 📦 O corpo da requisição deve ser um objeto JSON

  

```json
{
	"userName": "seu_nome_de_usuario",
	"email": "seu_email@exemplo.com",
	"password": "sua_senha"
}
```

---

  

# 🔓 Login e Geração de Token JWT

> Retorna um token JWT válido após autenticação com sucesso.

-   **Endpoint**: `POST /auth/login`
    
-   **URL completa**: `http://localhost:8080/auth/login`
    

### 📦 Corpo da Requisição (JSON)
  

```json
{
	"userName": "seu_nome_de_usuario",
	"password": "sua_senha"
}
```


### ✅ Exemplo de resposta

  

```json
{
	"token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtaWNoYWVsIiwiaWF0IjoxNzQ3MTAwNjg4LCJleHAiOjE3NDcxODcwODh9.tjLfXruC7TSDfU0NR5eiMggh-zUn6-NwiNx-eBHMsy8"
}
```

---

  

## 📬 Utilizando o Token JWT

  

Para autenticar requisições em endpoints protegidos, siga os passos abaixo:

  

**Copie o Token JWT** da resposta de login:

  
1.  Após o login, copie o valor do campo `token` retornado.
    
2.  No **Postman**, abra a aba **Authorization** da requisição desejada.
    
3.  Em **Type**, selecione `Bearer Token`.
    
4.  No campo **Token**, cole o JWT copiado:

```json
{
"token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtaWNoYWVsIiwiaWF0IjoxNzQ3MTAwNjg4LCJleHAiOjE3NDcxODcwODh9.tjLfXruC7TSDfU0NR5eiMggh-zUn6-NwiNx-eBHMsy8"
}
```
5. **Envie a requisição** e o servidor irá reconhecer seu token JWT e processar a solicitação conforme esperado.

---
# 📁  **Upload de Vídeo via URL com Postman, Processamento em Lote e Download Final**

Esta seção descreve o fluxo completo de **upload**, **processamento em lote** e **download** de vídeos utilizando o Postman com autenticação via JWT.

### 🔼 1. Upload de Vídeo

 **Selecione o método HTTP "POST"** no Postman.
 >Envia um vídeo para o servidor e retorna um ID único para uso nas etapas de processamento.

-   **Método**: `POST`
    
-   **Endpoint**: `http://localhost:8080/api/videos/upload`
    

### 🔐 Autorização

-   Vá até a aba **Authorization**
    
-   Selecione o tipo `Bearer Token`
    
-   Insira o token JWT obtido no login

### 💡 **Formato**: Utilize a aba **Body** → **form-data** no Postman.


### ✅ Quando a requisição for enviada, o servidor retornará uma resposta com o ID único do vídeo que foi enviado, algo como:

```json
{
	"success": [
		{
			"id": "145da6e6-765c-4139-b31a-ca4ee26d80fc",
			"fileName": "SampleVideo_1280x720_10mb_20250519_b7265ab65b6d4de1.mp4",
			"createdAt": "2025-05-19T20:00:59.697915757Z"
		}
	]
}
```
---

## ⚙️ 2. Processamento em Lote

Para iniciar o processamento em lote do vídeo, siga os seguintes passos:

> Realiza múltiplas operações de edição no vídeo previamente enviado.

-   **Método**: `POST`
    
-   **Endpoint**: `http://localhost:8080/api/videos/batch-process`
    

### 🔐 Autorização

-   Utilize o token JWT da mesma forma que no upload.
    

### 📦 Corpo da Requisição (JSON)

> Vá até a aba **Body**, selecione `raw` e escolha o tipo `JSON`.

  
```json
{

	"videoIds": [
		"a2aef537-4976-46b7-bef6-0f9d1bfe69cc"
	],
	"operations": [
		{
			"operationType": "CUT",
			"parameters": {
				"startTime": "00:00:32",
				"endTime": "00:01:02"
			}
		},
		{
			"operationType": "RESIZE",
			"parameters": {
				"width": 720,
				"height": 720
			}
		},
		{
			"operationType": "OVERLAY",
			"parameters": {
				"watermark": "Texto de marca-d'água",
				"position": "center",
				"fontSize": 20

			}
		},
		{
			"operationType": "CONVERT",
			"parameters": {
				"outputFormat": "avi"
			}
		}
	]
}
```
### ✅ Exemplo de Resposta:

 
```json
{
	"videoId": "8ac0e4f6-c6f5-4313-85f4-c76b46ddbfe8",
	"fileName": "SampleVideo_1280x720_10mb_20250513_8ea0f0d9780c4429_PROCESSED.avi",
	"createdAt": "2025-05-13T01:57:56.556600922Z",
	"operations": [
		"CUT",
		"RESIZE",
		"OVERLAY",
		"CONVERT"
	]
}
```


💡 **Observação:**  
O campo `operations` aceita **uma ou mais operações** a serem aplicadas no vídeo.  
Não é obrigatório enviar todas as operações no mesmo JSON — você pode enviar apenas uma (`CUT`), duas (`CUT` e `RESIZE`), ou até todas (`CUT`, `RESIZE`, `OVERLAY`, `CONVERT`), de acordo com a necessidade do processamento.

Os valores válidos para `operationType` são:

-   `CUT`
    
-   `RESIZE`
    
-   `OVERLAY`
    
-   `CONVERT`
---

# ⬇️ **Download de Vídeo Processado**

Após o processamento em lote ser concluído, você pode realizar o download do vídeo final. Siga os passos abaixo:

Após a conclusão do processamento em lote, é possível realizar o download do arquivo final gerado. Para isso:

1.  **Selecione o método HTTP `GET`** no Postman.
    
2.  Vá até a aba **"Authorization"** e selecione o tipo **Bearer Token**, colando seu **JWT**.
    
3.  **Digite a URL do endpoint** substituindo `{videoId}` pelo ID retornado no processamento:

	`http://localhost:8080/api/videos/download/{videoId}`

### 📌 **Exemplo completo:**

	http://localhost:8080/api/videos/download/8ac0e4f6-c6f5-4313-85f4-c76b46ddbfe8
-  Clique em **"Send and Download"** para iniciar o download

---
# 📄 Consulta de Vídeos Armazenados 

Este endpoint retorna uma lista com os vídeos enviados, suas datas de criação e status atual de processamento.

### ✅ Requisição:

-   **Método:** `GET`
    
-   **URL:** `http://localhost:8080/api/videos`
    
-   Vá até a aba **"Authorization"** e selecione o tipo **Bearer Token**, colando seu **JWT**.

### ✅ Exemplo de resposta:
```json
[
  {
    "videoFileName": "meu_video_incrivel.mp4",
    "createdTimes": "2024-05-21T10:30:00Z[UTC]",
    "status": "COMPLETED"
  },
  {
    "videoFileName": "tutorial_edição.mov",
    "createdTimes": "2024-05-20T15:45:10Z[UTC]",
    "status": "PROCESSING"
  },
  {
    "videoFileName": "viagem_ferias.avi",
    "createdTimes": "2024-05-19T08:00:05Z[UTC]",
    "status": "ERROR"
  }
]
```
