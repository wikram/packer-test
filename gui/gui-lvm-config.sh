#!/bin/bash

# ====== Disable firewall ==========
iptables -F 
systemctl disable firewalld
systemctl stop firewalld

#========== Disable selinux=============
sed -i 's/=enforcing/=disabled/g' /etc/selinux/config
sed -i 's/=permissive/=disabled/g' /etc/selinux/config
setenforce 0

#========== Extending the vg rootvg =======================
echo "Extending rootvg"
vgextend rootvg /dev/sdc

lvextend -L100G /dev/mapper/rootvg-homelv
resize2fs /dev/mapper/rootvg-homelv
lvextend -L8G /dev/mapper/rootvg-optlv
resize2fs /dev/mapper/rootvg-optlv

#========== Creating seperate partition for /var/log =========

echo "Creating seperate partition for /var/log"
lvcreate -L20G -n loglv rootvg
mkfs.ext4 -m 0 /dev/rootvg/loglv
mount /dev/rootvg/loglv /mnt
mv /var/log/* /mnt
umount /mnt
mount /dev/rootvg/loglv /var/log
echo '/dev/rootvg/loglv      /var/log       ext4      defaults     0 0' >> /etc/fstab

#============ Adding permissions on journalctl

/usr/bin/chmod u+s /bin/journalctl

#=========== Configure SSh for IPv4 ==========

sed -i 's/#AddressFamily any/AddressFamily inet/g' /etc/ssh/sshd_config
sed -i 's/#ListenAddress 0.0.0.0/ListenAddress 0.0.0.0/g' /etc/ssh/sshd_config
sed -i 's/#Port 22/Port 4020/g' /etc/ssh/sshd_config
sed -i 's/#LogLevel INFO/LogLevel INFO/g' /etc/ssh/sshd_config
sed -i 's/#MaxAuthTries 6/MaxAuthTries 4/g' /etc/ssh/sshd_config
sed -i 's/#IgnoreRhosts yes/IgnoreRhosts yes/g' /etc/ssh/sshd_config
sed -i 's/#HostbasedAuthentication no/HostbasedAuthentication no/g' /etc/ssh/sshd_config
sed -i 's/#PermitEmptyPasswords no/PermitEmptyPasswords no/g' /etc/ssh/sshd_config
sed -i 's/ClientAliveInterval 180/ClientAliveInterval 300/g' /etc/ssh/sshd_config
sed -i 's/#ClientAliveCountMax 3/ClientAliveCountMax 0/g' /etc/ssh/sshd_config
sed -i 's/#PermitRootLogin yes/PermitRootLogin no/g' /etc/ssh/sshd_config

# ============= FIS hardening ==============

cat >> /etc/sysctl.d/01-networking.conf << EOF
#
## FIS CIS Security Hardening
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.log_martians = 1
net.ipv4.conf.default.send_redirects = 0
net.ipv4.conf.all.send_redirects = 0 
net.ipv6.conf.all.accept_ra = 0
net.ipv6.conf.default.accept_ra = 0
net.ipv6.conf.default.accept_redirects = 0
net.ipv4.icmp_ignore_bogus_error_responses = 1
net.ipv4.conf.default.accept_source_route = 0
net.ipv6.conf.all.accept_ra = 0
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.all.secure_redirects = 0
net.ipv6.conf.all.accept_redirects = 0
net.ipv4.conf.all.log_martians = 1
net.ipv4.conf.default.secure_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv4.conf.all.accept_redirects = 0
fs.suid_dumpable = 0
#
EOF

cat >> /etc/sysctl.d/02-ipv6.conf << EOF
net.ipv6.conf.all.disable_ipv6 = 1
net.ipv6.conf.default.disable_ipv6 = 1
EOF

# ============ Configure EPEL repo ===========

cat > /etc/yum.repos.d/epel.repo << EOF
[epel]
name=Extra Packages for Enterprise Linux 7 - \$basearch
#baseurl=http://download.fedoraproject.org/pub/epel/7/\$basearch
metalink=https://mirrors.fedoraproject.org/metalink?repo=epel-7&arch=\$basearch
failovermethod=priority
enabled=1
gpgcheck=0
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7

[epel-debuginfo]
name=Extra Packages for Enterprise Linux 7 - \$basearch - Debug
#baseurl=http://download.fedoraproject.org/pub/epel/7/\$basearch/debug
metalink=https://mirrors.fedoraproject.org/metalink?repo=epel-debug-7&arch=\$basearch
failovermethod=priority
enabled=0
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7
gpgcheck=0

[epel-source]
name=Extra Packages for Enterprise Linux 7 - \$basearch - Source
#baseurl=http://download.fedoraproject.org/pub/epel/7/SRPMS
metalink=https://mirrors.fedoraproject.org/metalink?repo=epel-source-7&arch=\$basearch
failovermethod=priority
enabled=0
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7
gpgcheck=0
EOF

cat > /etc/yum.repos.d/azure-cli.repo << EOF
[azure-cli]
name=Azure CLI
baseurl=https://packages.microsoft.com/yumrepos/azure-cli
enabled=1
gpgcheck=1
gpgkey=https://packages.microsoft.com/keys/microsoft.asc
EOF

# ============ Configure ISCSI and NFS ====================

mkdir /ist-shared

# ============= upgrade VM ==========

yum update -y
yum upgrade -y
