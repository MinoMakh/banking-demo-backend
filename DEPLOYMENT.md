# Backend Deployment — AWS Free Tier (live runbook)

This is the **what we actually did**, not a sketch. If the EC2 instance gets nuked, follow this top to bottom and you'll be back up.

## At a glance

- **Live URL:** http://13.63.113.164:8080
- **Provider:** AWS Free Tier (12-month / Free Plan credits)
- **Region:** `eu-central-1` (Frankfurt)
- **Instance:** `banking-demo` — EC2 `t2.micro` (1 vCPU, 1 GB RAM), Amazon Linux 2023, 20 GB gp3
- **Elastic IP:** `13.63.113.164` (allocated + associated to the instance — release if you ever stop the instance, or AWS bills ~$3.60/mo)
- **Stack on the box:** MySQL 8.4 + Spring Boot 4.0.6, both in Docker, brought up by the unchanged `compose.yaml` (plus memory tweaks; see below)
- **Auth to the box:** SSH key `~/.ssh/banking-demo.pem` (RSA), user `ec2-user`
- **Auth to the API:** seed login `customerNo=C001` / `password=password`
- **Cost guardrail:** $1 zero-spend AWS Budget alarm

## Quick reference (the commands you'll re-run most)

```bash
# SSH in
export EIP=13.63.113.164
ssh -i ~/.ssh/banking-demo.pem ec2-user@$EIP

# Tail backend logs
ssh -i ~/.ssh/banking-demo.pem ec2-user@$EIP 'cd ~/banking && docker compose logs -f backend'

# Smoke test from your laptop
curl -X POST http://$EIP:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"customerNo":"C001","password":"password"}'

# Push local code changes to the server and rebuild
rsync -avz --exclude='target/' --exclude='.git/' --exclude='.idea/' \
  ~/Documents/Banking/backend/ ec2-user@$EIP:~/banking/
ssh -i ~/.ssh/banking-demo.pem ec2-user@$EIP 'cd ~/banking && docker compose up --build -d'
```

---

## Part 1 — One-time AWS account setup

### 1.1 Create the account

1. Sign up at `aws.amazon.com`. Needs a payment card for verification; the free tier covers everything here so the card should not be charged.
2. Enable MFA on the **root** account (IAM > Security credentials > Multi-factor authentication > Add MFA).
3. Create an IAM user with `AdministratorAccess` for daily use; **stop using root** after this.
4. Switch region to **eu-central-1 (Frankfurt)** via the top-right region dropdown.

### 1.2 Cost guardrail (do this BEFORE provisioning anything)

Billing > **Budgets** > Create budget > Use the *Zero-spend* template (or custom $1 monthly). Set the email to your real address. If anything starts costing money you'll know within hours.

> AWS introduced a "Free Plan" in 2024 — new accounts get $100 of credits for ~6 months instead of the classic 12-month tier. Either is more than enough here. Upgrade to the Paid Plan later if you want the classic 12-month free tier window.

### 1.3 SSH keypair

EC2 console > Network & Security > Key Pairs > **Create key pair**
- Name: `banking-demo`
- Type: RSA, format: `.pem`
- Download → save to `~/.ssh/banking-demo.pem` on your laptop
- `chmod 400 ~/.ssh/banking-demo.pem`  (SSH refuses keys with looser permissions)

---

## Part 2 — Provision the EC2 instance

### 2.1 Launch instance

EC2 console > **Launch instance**
- Name: `banking-demo`
- AMI: **Amazon Linux 2023** (free-tier eligible)
- Instance type: **t2.micro** (1 vCPU / 1 GB)
- Key pair: `banking-demo`
- **Network settings > Edit > Create security group** `banking-demo-sg`:
  - **SSH (22)** — source: **My IP** (auto-detected; will need to update if your home IP changes)
  - **Custom TCP (8080)** — source: **Anywhere (0.0.0.0/0)**
  - **Do not open 3306.** MySQL stays internal to the Compose network.
- Storage: **20 GB gp3** (free tier allows up to 30 GB EBS)
- Launch

### 2.2 Elastic IP

EC2 > **Elastic IPs** > Allocate > then **Associate** to the `banking-demo` instance.

> ⚠️ Elastic IPs are free **only while attached to a running instance**. If you ever *stop* (not just reboot) the instance, **release the EIP first** or you'll be charged ~$3.60/month for the unattached address.

The IP allocated here: **13.63.113.164**.

---

## Part 3 — Server prep (everything runs on the EC2)

SSH in:

```bash
ssh -i ~/.ssh/banking-demo.pem ec2-user@13.63.113.164
```

First connection asks to confirm the fingerprint — type `yes`.

### 3.1 Update + swap

```bash
sudo dnf update -y

# 2 GB swap — REQUIRED. 1 GB RAM is too tight for the Maven build; without swap
# the kernel OOM-kills the build halfway through.
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Verify with `free -h` — should show ~2 GiB of swap.

### 3.2 Docker + git

```bash
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
# Re-login so the docker group takes effect
exit
ssh -i ~/.ssh/banking-demo.pem ec2-user@13.63.113.164
```

### 3.3 Docker Compose v2 plugin

Amazon Linux 2023's `dnf` doesn't ship the Compose v2 plugin, so install it manually:

```bash
mkdir -p ~/.docker/cli-plugins
curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o ~/.docker/cli-plugins/docker-compose
chmod +x ~/.docker/cli-plugins/docker-compose
docker compose version  # sanity check
```

### 3.4 Docker Buildx v0.19+ (REQUIRED — see Troubleshooting #1)

The buildx that ships with Amazon Linux's bundled Docker is too old (<0.17), and the new Compose CLI refuses to build with it. Install a recent buildx the same way:

```bash
curl -SL https://github.com/docker/buildx/releases/download/v0.19.3/buildx-v0.19.3.linux-amd64 \
  -o ~/.docker/cli-plugins/docker-buildx
chmod +x ~/.docker/cli-plugins/docker-buildx
docker buildx version  # should print v0.19.3
```

---

## Part 4 — Get the project onto the server

From your **laptop** (not the EC2):

```bash
rsync -avz --exclude='target/' --exclude='.git/' --exclude='.idea/' \
  ~/Documents/Banking/backend/ ec2-user@13.63.113.164:~/banking/
```

The `:Z` SELinux suffix in `compose.yaml`'s db-init bind mount is harmless on Amazon Linux 2023 (which uses SELinux too, in permissive mode by default) — leave it alone.

---

## Part 5 — Server-side config

### 5.1 `.env` (on the EC2, never committed)

```bash
cd ~/banking
cat > .env <<EOF
MYSQL_DATABASE=banking
JWT_SECRET=$(openssl rand -base64 32)
EOF
chmod 600 .env
```

The `compose.yaml` references `${MYSQL_DATABASE}` and `${JWT_SECRET}` — Docker Compose reads them from this `.env` automatically.

### 5.2 Memory tweaks in `compose.yaml`

`t2.micro` only has 1 GB RAM. Without limits, MySQL and the JVM both grow until the kernel kills one. These caps are safe for local dev too and can also be committed to the local copy.

On the EC2, the merged `compose.yaml` should look like this (the deltas from local are `command:` on `mysql` and `JAVA_TOOL_OPTIONS` on `backend`):

```yaml
services:
  mysql:
    image: mysql:8.4
    command:
      - --innodb-buffer-pool-size=128M
      - --max-connections=20
    restart: unless-stopped
    environment:
      MYSQL_ALLOW_EMPTY_PASSWORD: "yes"
      MYSQL_DATABASE: ${MYSQL_DATABASE}
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./db/init:/docker-entrypoint-initdb.d:ro,Z
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      timeout: 5s
      retries: 10

  backend:
    build: .
    restart: unless-stopped
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/banking?useSSL=false&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ""
      JWT_SECRET: ${JWT_SECRET}
      JAVA_TOOL_OPTIONS: "-Xmx384m -Xms128m"

volumes:
  mysql_data:
```

> **Editing YAML on the server, the safe way:** don't open the file with `nano`/`vi` and re-indent — that's how the tab issue in Troubleshooting #2 happened. Overwrite via a **quoted-EOF heredoc** so bash doesn't expand `${...}` placeholders before Compose sees them:
> ```bash
> cat > ~/banking/compose.yaml <<'EOF'
> ... full file contents above ...
> EOF
> docker compose config   # validate before bringing things up
> ```

---

## Part 6 — Build + run

```bash
cd ~/banking
docker compose up --build -d
docker compose logs -f backend
```

The **first** Maven build takes 8–15 minutes on t2.micro and hits swap heavily — expected. Subsequent `docker compose up -d` (without `--build`) restarts in <30s.

Wait for `Started BackendApplication in N seconds`.

---

## Part 7 — Smoke test (from your laptop)

```bash
EIP=13.63.113.164

# Login
TOKEN=$(curl -s -X POST http://$EIP:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"customerNo":"C001","password":"password"}' | jq -r .token)
echo "$TOKEN"

# Authenticated endpoint
curl -s http://$EIP:8080/api/dashboard/summary -H "Authorization: Bearer $TOKEN" | jq
```

---

## Part 8 — Point the Flutter app at the deployed backend

```bash
cd ~/Documents/Banking/banking_system
flutter run --dart-define=API_BASE_URL=http://13.63.113.164:8080
```

Works on desktop / iOS sim / web. On a **physical Android** device you'll likely hit the cleartext-traffic block (Android disallows plain HTTP by default). Two options:

- Use the iOS simulator, desktop, or web target.
- Or add `android:usesCleartextTraffic="true"` to the `<application>` tag in `banking_system/android/app/src/main/AndroidManifest.xml` for the demo build only.

---

## Day-2 operations

### Redeploy after a local code change

From the laptop:

```bash
rsync -avz --exclude='target/' --exclude='.git/' --exclude='.idea/' \
  ~/Documents/Banking/backend/ ec2-user@13.63.113.164:~/banking/
ssh -i ~/.ssh/banking-demo.pem ec2-user@13.63.113.164 \
  'cd ~/banking && docker compose up --build -d'
```

Maven re-uses its local repo cache, so subsequent builds are 1–3 minutes (not 15).

### Restart without rebuilding

```bash
ssh -i ~/.ssh/banking-demo.pem ec2-user@13.63.113.164 \
  'cd ~/banking && docker compose restart backend'
```

### View logs

```bash
ssh -i ~/.ssh/banking-demo.pem ec2-user@13.63.113.164 \
  'cd ~/banking && docker compose logs -f backend'
# (Ctrl-C kills the tail, the container keeps running.)
```

### Re-run DB init scripts (wipe data)

After editing `db/init/*.sql`, MySQL **skips** the init scripts on subsequent boots because the data volume already exists. You have to wipe the volume:

```bash
ssh -i ~/.ssh/banking-demo.pem ec2-user@13.63.113.164 \
  'cd ~/banking && docker compose down -v && docker compose up -d'
```

### SQL shell on the server

```bash
ssh -i ~/.ssh/banking-demo.pem ec2-user@13.63.113.164 \
  'cd ~/banking && docker compose exec mysql mysql -uroot banking'
```

### Stop the instance overnight (optional, to save free-tier hours)

EC2 console > Instances > select instance > Instance state > **Stop**.

> **Before stopping, release the Elastic IP** (EC2 > Elastic IPs > select > Disassociate, then Release). Otherwise AWS charges ~$3.60/month for the unattached IP. You'll get a new IP next time you re-allocate.

---

## Troubleshooting log (issues we actually hit)

### #1 `compose build requires buildx 0.17.0 or later`

**Cause:** Amazon Linux 2023's bundled `docker-buildx` plugin is too old.
**Fix:** Drop a current buildx binary into `~/.docker/cli-plugins/docker-buildx`. See §3.4 above.

### #2 `yaml: line 4, column 13: mapping values are not allowed in this context`

**Cause:** An in-place `nano` edit of `compose.yaml` mixed in a tab character (YAML disallows tabs for indentation), or the new `command:` block's indent didn't line up with the surrounding mapping.
**Fix:** Overwrite the file with a **quoted-EOF** heredoc (`<<'EOF'`) so bash doesn't expand `${MYSQL_DATABASE}` / `${JWT_SECRET}` before writing, then `docker compose config` to validate. See the heredoc snippet in §5.2.
**Diagnosis aid:** `cat -A compose.yaml | head -25` reveals tabs as `^I` and line endings as `$`.

---

## Cost guardrails (re-read before you change anything)

- **Free tier:** 750 h/month t2.micro for 12 months (one always-on instance ≈ 730 h/month — under budget). 30 GB EBS. **1 GB outbound data/month free**, then $0.09/GB.
- The $1 budget alarm from §1.2 is your safety net. If you ever get a billing email, log in immediately and check the Bills page.
- **EIP-while-stopped** is the most common surprise charge — see the §4 / Day-2 callouts.
- **Don't open port 3306** to the internet, ever. MySQL is reached internally via the Compose network (`mysql:3306`).
- **Never commit `.env`.** It contains `JWT_SECRET`. The project `.gitignore` should cover it; double-check.

---

## What this deployment does NOT include (deliberate)

- HTTPS / custom domain (plain HTTP for demo only — fine for the work review).
- Managed RDS MySQL (in-Docker MySQL chosen so the implementation didn't have to change).
- CI/CD — deploys are manual `rsync` + `docker compose up`.
- Monitoring beyond the $1 billing alarm and `docker compose logs`.

If any of the above ever become requirements, add a Phase 5.1 to `banking_system/plan.md` first — don't slip them in here.
